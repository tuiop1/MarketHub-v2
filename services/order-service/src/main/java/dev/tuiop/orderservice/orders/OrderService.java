package dev.tuiop.orderservice.orders;


import dev.tuiop.orderservice.carts.CartServiceClient;
import dev.tuiop.orderservice.carts.dto.CartItemResponse;
import dev.tuiop.orderservice.carts.dto.CartResponse;
import dev.tuiop.orderservice.carts.exceptions.CartServiceException;
import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.customers.AccountCustomerClient;
import dev.tuiop.orderservice.customers.CustomerResponse;
import dev.tuiop.orderservice.customers.exceptions.AccountServiceException;
import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.exceptions.EmptyCartException;
import dev.tuiop.orderservice.orders.exceptions.OrderNotPendingPaymentException;
import dev.tuiop.orderservice.orders.mapper.OrderMapper;
import dev.tuiop.orderservice.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.payments.PaymentMethod;
import dev.tuiop.orderservice.payments.PaymentResultResponse;
import dev.tuiop.orderservice.payments.PaymentStatus;
import dev.tuiop.orderservice.payments.client.PaymentServiceClient;
import dev.tuiop.orderservice.products.CatalogProductClient;
import dev.tuiop.orderservice.products.ProductResponse;
import dev.tuiop.orderservice.products.ProductStockDecreaseRequest;
import dev.tuiop.orderservice.products.ProductStockIncreaseRequest;
import dev.tuiop.orderservice.products.exceptions.CatalogServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AccountCustomerClient accountCustomerClient;
    private final CatalogProductClient catalogProductClient;
    private final CartServiceClient cartServiceClient;
    private final PaymentServiceClient paymentServiceClient;



    @Transactional
    public Order createPendingOrder(
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

        for(ProductResponse product : products) {
            Integer quantity = quantitiesByProductId.get(product.id());
            OrderItem item = OrderItem.fromProduct(product, quantity);
            order.addItem(item);
        }
            order.recalculateTotalPrice();

            return orderRepository.save(order);




    }


    @Transactional
    public Order attachPayment(UUID orderId, UUID paymentId) {
        Order order = getOrder(orderId);
        order.attachPayment(paymentId);
        return order;
    }

    @Transactional
    public Order markPaid(UUID orderId) {
        Order order = getOrder(orderId);
        order.markPaid();
        return order;
    }

    @Transactional
    public Order markPaymentFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.markPaymentFailed(reason);
        return order;
    }

    @Transactional
    public Order cancel(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        return order;
    }

    @Transactional
    public void markCompensationFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.markCompensationFailed(reason);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
    }










    public OrderResponse purchase(
            Jwt jwt,
            PurchaseRequest request

    ) {
        Map<UUID, Integer> quantitiesByProductId = mergeQuantitiesByProductId(request.items());
        Order order = createOrderAndPay(jwt, quantitiesByProductId, request.paymentMethod());

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse purchaseMyCart(
            Jwt jwt,
            PaymentMethod paymentMethod
    ) {
        getMe(jwt);

        CartResponse myCart = getMyCart(jwt);

        if (myCart.cartItems().isEmpty()) {
            throw new EmptyCartException();
        }

        Map<UUID, Integer> quantitiesByProductId = myCart.cartItems()
                .stream()
                .collect(Collectors.toMap(
                        CartItemResponse::productId,
                        CartItemResponse::quantity
                ));

        Order order = createOrderAndPay(jwt, quantitiesByProductId, paymentMethod);

        clearMyCart(jwt);

        return orderMapper.toOrderResponse(order);

    }

    private Order createOrder(
            Jwt jwt,
            Map<UUID, Integer> quantitiesByProductId
    ) {
        CustomerResponse customer = getMe(jwt);
        log.info(
                "Purchase requested: customerId={}, uniqueProductCount={}, totalRequestedItems={}",
                customer.id(),
                quantitiesByProductId.size(),
                quantitiesByProductId.values().stream().mapToInt(Integer::intValue).sum()
        );
        Map<UUID, ProductResponse> productsById = decreaseStock(quantitiesByProductId)
                .stream()
                .collect(Collectors.toMap(ProductResponse::id, Function.identity()));

        Order order = Order.builder()
                .customerId(customer.id())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalPriceCents(0L)
                .build();

        for (Map.Entry<UUID, Integer> entry : quantitiesByProductId.entrySet()) {
            ProductResponse product = productsById.get(entry.getKey());
            int quantity = entry.getValue();

            OrderItem orderItem = OrderItem.fromProduct(product, quantity);
            order.addItem(orderItem);
        }


        order.recalculateTotalPrice();

        Order savedOrder = orderRepository.save(order);
        log.info(
                "Order created: orderId={}, customerId={}, itemCount={}, totalPriceCents={}, status={}",
                savedOrder.getId(),
                customer.id(),
                savedOrder.getOrderItems().size(),
                savedOrder.getTotalPriceCents(),
                savedOrder.getStatus()
        );
        return savedOrder;

    }

    private PaymentResultResponse makePayment(Jwt jwt, UUID orderId, PaymentMethod paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));


        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotPendingPaymentException(orderId, order.getStatus());
        }

        return paymentServiceClient.createPayment(
                authorizationHeader(jwt),
                CreatePaymentRequest.builder()
                        .orderId(orderId)
                        .customerId(order.getCustomerId())
                        .amountCents(order.getTotalPriceCents())
                        .paymentMethod(paymentMethod)
                        .build()
        );
    }

    private Order createOrderAndPay(Jwt jwt,
            Map<UUID, Integer> quantitiesByProductId,
            PaymentMethod method
    ) {

        Order returnedOrder = createOrder(jwt, quantitiesByProductId);

        PaymentResultResponse paymentResultResponse = makePayment(jwt, returnedOrder.getId(), method);

        if (paymentResultResponse.status() == PaymentStatus.FAILED) {
            returnedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
            increaseStock(quantitiesByProductId);
        } else if (paymentResultResponse.status() == PaymentStatus.SUCCEEDED) {
            returnedOrder.setStatus(OrderStatus.PAID);
        }

        return orderRepository.save(returnedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Jwt jwt, Pageable pageable) {

        CustomerResponse customerResponse = getMe(jwt);


        return orderRepository.findByCustomerId(customerResponse.id(), pageable)
                .map(orderMapper::toOrderResponse);
    }

    Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {

        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            quantitiesByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }


    private void validateAllProductsFound(
            Collection<UUID> requestedProductIds,
            Map<UUID, ProductResponse> productsById
    ) {
        for (UUID requestedProductId : requestedProductIds) {
            if (!productsById.containsKey(requestedProductId)) {
                log.warn(
                        "Purchase failed because product is not buyable or does not exist: productId={}",
                        requestedProductId
                );

                throw new ResourceNotFoundException(ProductResponse.class, requestedProductId);
            }
        }
    }




    private CartResponse getMyCart(Jwt jwt) {
        try {
            return cartServiceClient.getMyCart(authorizationHeader(jwt));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(CartResponse.class, "customer", "current");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Cart service authorization failed while getting my cart", exception);
            throw CartServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Cart service request failed while getting my cart", exception);
            throw CartServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Cart service client failed while getting my cart", exception);
            throw CartServiceException.unavailable(exception);
        }
    }

    private void clearMyCart(Jwt jwt) {
        try {
            cartServiceClient.clearMyCart(authorizationHeader(jwt));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(CartResponse.class, "customer", "current");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Cart service authorization failed while clearing my cart", exception);
            throw CartServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Cart service rejected clear cart request", exception);
            throw CartServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Cart service request failed while clearing my cart", exception);
            throw CartServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Cart service client failed while clearing my cart", exception);
            throw CartServiceException.unavailable(exception);
        }
    }

    private String authorizationHeader(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }



     CustomerResponse getMe(Jwt jwt) {
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
}
