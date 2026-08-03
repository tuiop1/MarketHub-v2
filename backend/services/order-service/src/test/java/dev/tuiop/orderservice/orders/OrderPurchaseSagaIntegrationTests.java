package dev.tuiop.orderservice.orders;

import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.orderservice.external.customers.AccountCustomerClient;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
import dev.tuiop.orderservice.external.payments.PaymentResultResponse;
import dev.tuiop.orderservice.external.payments.PaymentStatus;
import dev.tuiop.orderservice.external.payments.client.PaymentServiceClient;
import dev.tuiop.orderservice.external.products.CatalogStockReservationClient;
import dev.tuiop.orderservice.external.products.StockReservationItemResponse;
import dev.tuiop.orderservice.external.products.StockReservationRequest;
import dev.tuiop.orderservice.external.products.StockReservationResponse;
import dev.tuiop.orderservice.external.products.StockReservationStatus;
import dev.tuiop.orderservice.external.products.exceptions.CatalogServiceException;
import dev.tuiop.orderservice.kafka.OrderNotificationEventPublisher;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class OrderPurchaseSagaIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("orders_test")
            .withUsername("markethub")
            .withPassword("markethub");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
    }

    @Autowired
    private OrderPurchaseSagaService sagaService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private AccountCustomerClient accountCustomerClient;

    @MockitoBean
    private CatalogStockReservationClient catalogStockReservationClient;

    @MockitoBean
    private PaymentServiceClient paymentServiceClient;

    @MockitoBean
    private OrderNotificationEventPublisher eventPublisher;

    private UUID customerId;
    private UUID productId;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        reset(accountCustomerClient, catalogStockReservationClient, paymentServiceClient, eventPublisher);

        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        merchantId = UUID.randomUUID();

        when(accountCustomerClient.getMe(anyString())).thenReturn(customer());
        when(catalogStockReservationClient.reserveStock(any())).thenAnswer(invocation -> {
            StockReservationRequest request = invocation.getArgument(0);
            int quantity = request.items().getFirst().quantity();
            return reservation(request.reservationId(), quantity);
        });
    }

    @Test
    void successfulOrderCommitsStockPersistsPaymentAndPublishesEvent() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.SUCCEEDED, "approved"));

        Order result = sagaService.purchase(jwt(), purchase(2, PaymentMethod.CARD));

        Order persisted = orderRepository.findById(result.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.getPaymentId()).isEqualTo(paymentId);
        assertThat(persisted.getTotalPriceCents()).isEqualTo(2_500L);
        verify(catalogStockReservationClient).commitStock(persisted.getStockReservationId());
        verify(catalogStockReservationClient, never()).releaseStock(any());
        verify(paymentServiceClient, never()).cancelOrRefund(any());

        ArgumentCaptor<OrderConfirmedEvent> event = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(eventPublisher).publishOrderConfirmed(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(persisted.getId());
        assertThat(event.getValue().customerId()).isEqualTo(customerId);
        assertThat(event.getValue().totalPriceCents()).isEqualTo(2_500L);
    }

    @Test
    void failedPaymentReleasesStockAndMarksOrderFailed() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.FAILED, "declined"));

        Order result = sagaService.purchase(jwt(), purchase(1, PaymentMethod.QR));

        Order persisted = orderRepository.findById(result.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(persisted.getFailureReason()).isEqualTo("declined");
        verify(catalogStockReservationClient).releaseStock(persisted.getStockReservationId());
        verify(catalogStockReservationClient, never()).commitStock(any());
        verify(eventPublisher, never()).publishOrderConfirmed(any());
    }

    @Test
    void commitFailureRefundsPaymentReleasesStockAndCancelsOrder() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.SUCCEEDED, "approved"));
        doThrow(new RestClientException("commit unavailable"))
                .when(catalogStockReservationClient).commitStock(any());

        assertThatThrownBy(() -> sagaService.purchase(jwt(), purchase(1, PaymentMethod.CARD)))
                .isInstanceOf(CatalogServiceException.class);

        Order persisted = onlyOrder();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentServiceClient).cancelOrRefund(paymentId);
        verify(catalogStockReservationClient).releaseStock(persisted.getStockReservationId());
        verify(eventPublisher, never()).publishOrderConfirmed(any());
    }

    @Test
    void failedRefundMarksOrderAsCompensationFailed() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.SUCCEEDED, "approved"));
        doThrow(new RestClientException("commit unavailable"))
                .when(catalogStockReservationClient).commitStock(any());
        when(paymentServiceClient.cancelOrRefund(paymentId))
                .thenThrow(new RestClientException("refund unavailable"));

        assertThatThrownBy(() -> sagaService.purchase(jwt(), purchase(1, PaymentMethod.CARD)))
                .isInstanceOf(CatalogServiceException.class);

        assertThat(onlyOrder().getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
    }

    @Test
    void failedInitialReleaseIsRetriedDuringCompensation() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.FAILED, "declined"));
        doThrow(new RestClientException("temporary release failure"))
                .doNothing()
                .when(catalogStockReservationClient).releaseStock(any());

        assertThatThrownBy(() -> sagaService.purchase(jwt(), purchase(1, PaymentMethod.QR)))
                .isInstanceOf(CatalogServiceException.class);

        Order persisted = onlyOrder();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(catalogStockReservationClient, times(2)).releaseStock(persisted.getStockReservationId());
        verify(paymentServiceClient).cancelOrRefund(paymentId);
    }

    @Test
    void failedCompensationReleaseMarksOrderForManualRecovery() {
        UUID paymentId = UUID.randomUUID();
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(paymentId, PaymentStatus.SUCCEEDED, "approved"));
        doThrow(new RestClientException("commit unavailable"))
                .when(catalogStockReservationClient).commitStock(any());
        doThrow(new RestClientException("release unavailable"))
                .when(catalogStockReservationClient).releaseStock(any());

        assertThatThrownBy(() -> sagaService.purchase(jwt(), purchase(1, PaymentMethod.CARD)))
                .isInstanceOf(CatalogServiceException.class);

        Order persisted = onlyOrder();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(paymentServiceClient).cancelOrRefund(paymentId);
    }

    @Test
    void duplicateProductLinesAreMergedIntoOneReservationItem() {
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(new PaymentResultResponse(UUID.randomUUID(), PaymentStatus.SUCCEEDED, "approved"));
        PurchaseRequest request = new PurchaseRequest(
                List.of(
                        new PurchaseItemRequest(productId, 1),
                        new PurchaseItemRequest(productId, 2)
                ),
                PaymentMethod.CARD
        );

        Order result = sagaService.purchase(jwt(), request);

        ArgumentCaptor<StockReservationRequest> reservationRequest =
                ArgumentCaptor.forClass(StockReservationRequest.class);
        verify(catalogStockReservationClient).reserveStock(reservationRequest.capture());
        assertThat(reservationRequest.getValue().items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(productId);
            assertThat(item.quantity()).isEqualTo(3);
        });
        assertThat(result.getTotalPriceCents()).isEqualTo(3_750L);
    }

    private PurchaseRequest purchase(int quantity, PaymentMethod method) {
        return new PurchaseRequest(List.of(new PurchaseItemRequest(productId, quantity)), method);
    }

    private StockReservationResponse reservation(UUID reservationId, int quantity) {
        return new StockReservationResponse(
                reservationId,
                StockReservationStatus.RESERVED,
                List.of(new StockReservationItemResponse(
                        productId,
                        merchantId,
                        "Coffee",
                        1_250L,
                        quantity,
                        1_250L * quantity
                ))
        );
    }

    private CustomerResponse customer() {
        return new CustomerResponse(
                customerId,
                "Taylor",
                "Customer",
                null,
                "customer@example.com",
                null,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("customer-token")
                .header("alg", "none")
                .subject("keycloak-customer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private Order onlyOrder() {
        assertThat(orderRepository.count()).isOne();
        return orderRepository.findAll().getFirst();
    }
}
