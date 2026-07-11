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

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {

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


    private Collection<ProductResponse> decreaseStock(Map<UUID, Integer> quantitiesByProductId) {
        Collection<ProductStockDecreaseRequest> requests = quantitiesByProductId.entrySet()
                .stream()
                .map(entry -> new ProductStockDecreaseRequest(entry.getKey(), entry.getValue()))
                .toList();

        try {
            Collection<ProductResponse> products = catalogProductClient.decreaseStock(requests);
            Map<UUID, ProductResponse> productsById = products.stream()
                    .collect(Collectors.toMap(ProductResponse::id, Function.identity()));

            validateAllProductsFound(quantitiesByProductId.keySet(), productsById);

            return products;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(ProductResponse.class, "productIds", quantitiesByProductId.keySet());
        } catch (HttpClientErrorException.Conflict exception) {
            log.warn("Catalog service rejected stock decrease request: productIds={}", quantitiesByProductId.keySet(), exception);
            throw CatalogServiceException.rejected(exception);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while decreasing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while decreasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while decreasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private Collection<ProductResponse> increaseStock(Map<UUID, Integer> quantitiesByProductId) {
        Collection<ProductStockIncreaseRequest> requests = quantitiesByProductId.entrySet()
                .stream()
                .map(entry -> new ProductStockIncreaseRequest(entry.getKey(), entry.getValue()))
                .toList();

        try {
            Collection<ProductResponse> products = catalogProductClient.increaseStock(requests);
            Map<UUID, ProductResponse> productsById = products.stream()
                    .collect(Collectors.toMap(ProductResponse::id, Function.identity()));

            validateAllProductsFound(quantitiesByProductId.keySet(), productsById);

            return products;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(ProductResponse.class, "productIds", quantitiesByProductId.keySet());
        } catch (HttpClientErrorException.Conflict exception) {
            log.warn("Catalog service rejected stock increase request: productIds={}", quantitiesByProductId.keySet(), exception);
            throw CatalogServiceException.rejected(exception);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while increasing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while increasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while increasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
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
}
