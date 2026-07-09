package dev.tuiop.orderservice.orders;


import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.customers.AccountCustomerClient;
import dev.tuiop.orderservice.customers.CustomerResponse;
import dev.tuiop.orderservice.customers.exceptions.AccountServiceException;
import dev.tuiop.orderservice.orders.dto.OrderResponse;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.enums.PaymentStatus;
import dev.tuiop.orderservice.orders.mapper.OrderMapper;
import dev.tuiop.orderservice.products.CatalogProductClient;
import dev.tuiop.orderservice.products.ProductResponse;
import dev.tuiop.orderservice.products.ProductStockDecreaseRequest;
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

    @Transactional
    public OrderResponse purchase(
            Jwt jwt,
            PurchaseRequest request
    ) {
       return orderMapper.toOrderResponse(createOrder(jwt, mergeQuantitiesByProductId(request.items())));

    }
    @Transactional
    public OrderResponse purchaseMyCart(
            Jwt jwt
    ) {
        UUID userId = jwt.getUserId();

        Cart myCart = cartRepository.findDetailedByUserIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException(Cart.class, "user.id",userId));

            if(myCart.getCartItems().isEmpty()){
                throw new EmptyCartException();
            }

        Map<UUID, Integer> quantitiesByProductId = myCart.getCartItems().stream().collect(Collectors.toMap(
                userItem -> userItem.getProduct().getId(),
                CartItem::getQuantity
        ));

        Order order = createOrder(jwt, quantitiesByProductId);

        myCart.getCartItems().clear();

        return orderMapper.toOrderResponse(order);

    }

    private Order createOrder(
            Jwt jwt,
            Map<UUID, Integer> quantitiesByProductId
    ) {
        String keycloakId = jwt.getSubject();;
        CustomerResponse customer = getCustomerByKeycloakUserId(keycloakId);
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
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
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
                "Order created: orderId={}, customerId={}, itemCount={}, totalPriceCents={}, status={}, paymentStatus={}",
                savedOrder.getId(),
                customer.id(),
                savedOrder.getOrderItems().size(),
                savedOrder.getTotalPriceCents(),
                savedOrder.getStatus(),
                savedOrder.getPaymentStatus()
        );
        return savedOrder;

    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Jwt jwt, Pageable pageable) {

        CustomerResponse customerResponse = getCustomerByKeycloakUserId(jwt.getSubject());


        return orderRepository.findByCustomerId(customerResponse.id(), pageable)
                .map(orderMapper::toOrderResponse);
    }

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {

        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            UUID id = item.productId();
            Integer quantity = item.quantity();

            if (!quantitiesByProductId.containsKey(id)) {
                quantitiesByProductId.put(id, quantity);
            } else {
                quantitiesByProductId.put(id, quantitiesByProductId.get(id) +  quantity);
            }

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



    private CustomerResponse getCustomerByKeycloakUserId(String keycloakId) {
        try {
            return accountCustomerClient.getCustomerByKeycloakUserId(keycloakId);
        } catch (HttpClientErrorException.NotFound exception ){
            throw new ResourceNotFoundException(CustomerResponse.class, "keycloakId", keycloakId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }
}
