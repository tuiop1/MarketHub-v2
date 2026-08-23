package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.external.carts.CartServiceClient;
import dev.tuiop.orderservice.external.carts.dto.CartItemResponse;
import dev.tuiop.orderservice.external.carts.dto.CartResponse;
import dev.tuiop.orderservice.external.customers.AccountCustomerClient;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.exceptions.EmptyCartException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AccountCustomerClient accountCustomerClient;
    @Mock
    private CartServiceClient cartServiceClient;
    @Mock
    private OrderPurchaseSagaService orderPurchaseSagaService;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldNotStartPurchaseWhenCartIsEmpty() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        CustomerResponse customer = customer(customerId);
        CartResponse emptyCart = new CartResponse(
                UUID.randomUUID(), customerId, 0L, List.of()
        );

        when(jwt.getTokenValue()).thenReturn("someToken");
        when(accountCustomerClient.getMe("Bearer someToken")).thenReturn(customer);
        when(cartServiceClient.getMyCart("Bearer someToken")).thenReturn(emptyCart);

        // Act
        assertThrows(
                EmptyCartException.class,
                () -> orderService.purchaseMyCart(jwt, PaymentMethod.CARD)
        );

        // Assert
        verifyNoInteractions(orderPurchaseSagaService);
        verify(cartServiceClient, never()).clearMyCart(any());
    }

    @Test
    void shouldPurchaseItemsAndClearCartWhenPaymentSucceeds() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CartItemResponse cartItem = new CartItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                productId,
                "Coffee beans",
                1_299L,
                2,
                2_598L,
                true,
                8
        );
        CartResponse cart = new CartResponse(
                cartItem.cartId(), customerId, 2_598L, List.of(cartItem)
        );
        Order paidOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .status(OrderStatus.PAID)
                .totalPriceCents(2_598L)
                .build();

        when(jwt.getTokenValue()).thenReturn("someToken");
        when(accountCustomerClient.getMe("Bearer someToken")).thenReturn(customer(customerId));
        when(cartServiceClient.getMyCart("Bearer someToken")).thenReturn(cart);
        when(orderPurchaseSagaService.purchase(eq(jwt), any(PurchaseRequest.class)))
                .thenReturn(paidOrder);

        // Act
        Order result = orderService.purchaseMyCart(jwt, PaymentMethod.CARD);

        // Assert
        assertSame(paidOrder, result);

        ArgumentCaptor<PurchaseRequest> requestCaptor =
                ArgumentCaptor.forClass(PurchaseRequest.class);
        verify(orderPurchaseSagaService).purchase(eq(jwt), requestCaptor.capture());

        PurchaseRequest purchaseRequest = requestCaptor.getValue();
        assertEquals(PaymentMethod.CARD, purchaseRequest.paymentMethod());
        assertEquals(1, purchaseRequest.items().size());
        assertEquals(productId, purchaseRequest.items().getFirst().productId());
        assertEquals(2, purchaseRequest.items().getFirst().quantity());
        verify(cartServiceClient).clearMyCart("Bearer someToken");
    }

    private CustomerResponse customer(UUID customerId) {
        return new CustomerResponse(
                customerId,
                "John",
                "Doe",
                null,
                "john@gmail.com",
                null,
                true,
                null,
                null
        );
    }
}
