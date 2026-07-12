package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.customers.AccountCustomerClient;
import dev.tuiop.orderservice.customers.CustomerResponse;
import dev.tuiop.orderservice.customers.exceptions.AccountServiceException;
import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.mapper.OrderMapper;
import dev.tuiop.orderservice.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.payments.PaymentMethod;
import dev.tuiop.orderservice.payments.PaymentResultResponse;
import dev.tuiop.orderservice.payments.PaymentStatus;
import dev.tuiop.orderservice.payments.client.PaymentServiceClient;
import dev.tuiop.orderservice.payments.exceptions.PaymentServiceException;
import dev.tuiop.orderservice.products.CatalogStockReservationClient;
import dev.tuiop.orderservice.products.ProductResponse;
import dev.tuiop.orderservice.products.StockReservationItemRequest;
import dev.tuiop.orderservice.products.StockReservationItemResponse;
import dev.tuiop.orderservice.products.StockReservationRequest;
import dev.tuiop.orderservice.products.StockReservationResponse;
import dev.tuiop.orderservice.products.exceptions.CatalogServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPurchaseSagaService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AccountCustomerClient accountCustomerClient;
    private final CatalogStockReservationClient catalogStockReservationClient;
    private final PaymentServiceClient paymentServiceClient;

    public OrderResponse purchase(Jwt jwt, PurchaseRequest purchaseRequest) {
        UUID stockReservationId = UUID.randomUUID();

        Map<UUID, Integer> quantitiesByProductId =
                mergeQuantitiesByProductId(purchaseRequest.items());

        CustomerResponse customerResponse = getMe(jwt);

        List<StockReservationItemRequest> items = quantitiesByProductId.keySet()
                .stream()
                .map(id -> new StockReservationItemRequest(id, quantitiesByProductId.get(id)))
                .toList();

        StockReservationRequest request = StockReservationRequest.builder()
                .reservationId(stockReservationId)
                .items(items)
                .build();

        StockReservationResponse reservation = null;
        Order order = null;
        PaymentResultResponse payment = null;
        try {

            reservation = reserveStock(request);

            order = createPendingOrder(
                    customerResponse.id(),
                    stockReservationId,
                    toProductResponses(reservation.items()),
                    quantitiesByProductId
            );

            payment = createPayment(jwt, order, purchaseRequest.paymentMethod());

            attachPayment(order.getId(), payment.paymentId());

            if (payment.status() == PaymentStatus.FAILED) {
                releaseStockSafely(stockReservationId);

                Order failedOrder = markPaymentFailed(order.getId(), payment.info());

                return orderMapper.toOrderResponse(failedOrder);
            }

            if (payment.status() == PaymentStatus.SUCCEEDED) {

                commitStockSafely(stockReservationId);



                Order paidOrder = markPaid(order.getId());

                return orderMapper.toOrderResponse(paidOrder);

            }

            throw new IllegalArgumentException("Unsupported payment status: " + payment.status());
        } catch(Exception exception){

            compensateAfterUnexpectedFailure(order, payment, stockReservationId, exception, jwt);

            throw  exception;

        }
    }


    //compensation logic

    private void compensateAfterUnexpectedFailure(Order order, PaymentResultResponse payment,
                                                  UUID stockReservationId,
                                                  Exception originalException,
                                                  Jwt jwt){

        log.warn(
                "Purchase saga failed. Starting compensation: orderId={}, paymentId={}, stockReservationId={}",
                order != null ? order.getId() : null,
                payment != null ? payment.paymentId() : null,
                stockReservationId,
                originalException
        );

        boolean compensationFailed = false;


        if(payment != null && payment.paymentId() != null){
            try{
                paymentServiceClient.cancelOrRefund(authorizationHeader(jwt),payment.paymentId());

            } catch (Exception e) {
                compensationFailed = true;
                log.error(
                        "Payment compensation failed: paymentId={}",
                        payment.paymentId(),
                        e
                );
            }
        }

        try {
            releaseStockSafely(stockReservationId);
        }
        catch (Exception e ){
            compensationFailed = true;
            log.error(
                    "Stock compensation failed: reservationId={}",
                    stockReservationId,
                    e
            );
        }
        if(order != null){
            try{
                if (compensationFailed){
                    markCompensationFailed(order.getId(), originalException.getMessage());

                }
                else{
                    cancel(order.getId(), originalException.getMessage());
                }
            } catch (Exception e) {
                log.error(
                        "Could not update order after saga compensation: orderId={}",
                        order.getId(),
                        e
                );
            }
        }

    }

    private Order createPendingOrder(
            UUID customerId,
            UUID stockReservationId,
            Collection<ProductResponse> products,
            Map<UUID, Integer> quantitiesByProductId
    ) {

        Order order = Order.builder()
                .customerId(customerId)
                .stockReservationId(stockReservationId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalPriceCents(0L)
                .build();

        for (ProductResponse product : products) {
            Integer quantity = quantitiesByProductId.get(product.id());
            OrderItem item = OrderItem.fromProduct(product, quantity);
            order.addItem(item);
        }

        order.recalculateTotalPrice();

        return orderRepository.save(order);
    }

    private Order attachPayment(UUID orderId, UUID paymentId) {
        Order order = getOrder(orderId);
        order.attachPayment(paymentId);
        return orderRepository.save(order);
    }


    public Order markPaid(UUID orderId) {
        Order order = getOrder(orderId);
        order.markPaid();
        return orderRepository.save(order);
    }

    public Order markPaymentFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.markPaymentFailed(reason);
        return orderRepository.save(order);
    }

    public Order cancel(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        return orderRepository.save(order);
    }

    public void markCompensationFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.markCompensationFailed(reason);
        orderRepository.save(order);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
    }

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {
        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            quantitiesByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }

    private CustomerResponse getMe(Jwt jwt) {
        try {
            return accountCustomerClient.getMe(authorizationHeader(jwt));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(CustomerResponse.class, "current");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting current customer", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }


    private PaymentResultResponse createPayment(Jwt jwt, Order order, PaymentMethod paymentMethod) {
        try {
            return paymentServiceClient.createPayment(
                    authorizationHeader(jwt),
                    CreatePaymentRequest.builder()
                            .orderId(order.getId())
                            .customerId(order.getCustomerId())
                            .amountCents(order.getTotalPriceCents())
                            .paymentMethod(paymentMethod)
                            .build()
            );
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Payment service authorization failed while creating payment", exception);
            throw PaymentServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Payment service rejected create payment request", exception);
            throw PaymentServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Payment service request failed while creating payment", exception);
            throw PaymentServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Payment service client failed while creating payment", exception);
            throw PaymentServiceException.unavailable(exception);
        }
    }

    private String authorizationHeader(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }

    private StockReservationResponse reserveStock(StockReservationRequest request) {
        try {
            return catalogStockReservationClient.reserveStock(request);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while reserving stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock reservation request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while reserving stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while reserving stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private void releaseStockSafely(UUID stockReservationId) {
        try {
            catalogStockReservationClient.releaseStock(stockReservationId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while releasing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock release request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while releasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while releasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private void commitStockSafely(UUID stockReservationId) {
        try {
            catalogStockReservationClient.commitStock(stockReservationId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while committing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock commit request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while committing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while committing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private List<ProductResponse> toProductResponses(Collection<StockReservationItemResponse> items) {
        return items.stream()
                .map(this::toProductResponse)
                .toList();
    }

    private ProductResponse toProductResponse(StockReservationItemResponse item) {
        return new ProductResponse(
                item.productId(),
                item.merchantId(),
                "",
                null,
                item.productName(),
                null,
                item.unitPriceCents(),
                null,
                true,
                null,
                null
        );
    }

}
