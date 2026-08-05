package dev.tuiop.orderservice.orders;


import dev.tuiop.orderservice.external.carts.CartServiceClient;
import dev.tuiop.orderservice.external.carts.dto.CartResponse;
import dev.tuiop.orderservice.external.carts.exceptions.CartServiceException;
import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.external.customers.AccountCustomerClient;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.external.customers.exceptions.AccountServiceException;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.exceptions.EmptyCartException;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountCustomerClient accountCustomerClient;
    private final CartServiceClient cartServiceClient;
    private final OrderPurchaseSagaService orderPurchaseSagaService;

    public Order purchase(
            Jwt jwt,
            PurchaseRequest request
    ) {
        return orderPurchaseSagaService.purchase(jwt, request);
    }

    public Order purchaseMyCart(
            Jwt jwt,
            PaymentMethod paymentMethod
    ) {
        getMe(jwt);

        CartResponse myCart = getMyCart(jwt);

        if (myCart.cartItems().isEmpty()) {
            throw new EmptyCartException();
        }

        List<PurchaseItemRequest> items = myCart.cartItems()
                .stream()
                .map(item -> new PurchaseItemRequest(item.productId(), item.quantity()))
                .toList();

        Order order = orderPurchaseSagaService.purchase(jwt, new PurchaseRequest(items, paymentMethod));

            try{
                if(order.getStatus() == OrderStatus.PAID){
                    clearMyCart(jwt);
                }
            }
            catch (Exception e ){
                log.warn("Order created, but cart clearing failed: orderId={}", order.getId(), e);
            }



        return order;

    }



    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(Jwt jwt, Pageable pageable) {

        CustomerResponse customerResponse = getMe(jwt);


        return orderRepository.findByCustomerId(customerResponse.id(), pageable);
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
