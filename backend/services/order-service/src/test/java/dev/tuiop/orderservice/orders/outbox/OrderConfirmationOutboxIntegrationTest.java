package dev.tuiop.orderservice.orders.outbox;

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
import dev.tuiop.orderservice.kafka.OrderNotificationEventPublisher;
import dev.tuiop.orderservice.orders.Order;
import dev.tuiop.orderservice.orders.OrderItem;
import dev.tuiop.orderservice.orders.OrderPurchaseSagaService;
import dev.tuiop.orderservice.orders.OrderRepository;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({OrderPaymentCompletionService.class, OrderConfirmationOutboxPublisher.class, OrderPurchaseSagaService.class,
        OrderConfirmationOutboxIntegrationTest.JsonConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIf("databaseAvailable")
class OrderConfirmationOutboxIntegrationTest {
    private static PostgreSQLContainer postgres;

    @Autowired OrderRepository orders;
    @Autowired OrderConfirmationOutboxRepository outbox;
    @Autowired OrderPaymentCompletionService completion;
    @Autowired OrderPurchaseSagaService purchaseService;
    @Autowired OrderConfirmationOutboxPublisher outboxPublisher;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean OrderNotificationEventPublisher publisher;
    @MockitoBean AccountCustomerClient customers;
    @MockitoBean CatalogStockReservationClient catalog;
    @MockitoBean PaymentServiceClient payments;

    static boolean databaseAvailable() {
        return System.getenv("OUTBOX_TEST_DATABASE_URL") != null
                || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("OUTBOX_TEST_DATABASE_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv("OUTBOX_TEST_DATABASE_USER"));
            registry.add("spring.datasource.password", () -> "");
        } else {
            postgres = new PostgreSQLContainer("postgres:18-alpine");
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void stopDatabase() {
        if (postgres != null) postgres.stop();
    }

    @BeforeEach
    void clearDatabase() {
        outbox.deleteAllInBatch();
        orders.deleteAll();
    }

    @Test
    void savesPaidOrderAndCompleteSnapshotWithoutCallingKafka() {
        Order pending = pendingOrder();
        Order paid = complete(pending);

        assertThat(orders.findById(paid.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        OrderConfirmationOutbox entry = outbox.findById(paid.getId()).orElseThrow();
        OrderConfirmedEvent event = mapper.readValue(entry.getPayload(), OrderConfirmedEvent.class);
        assertThat(entry.getPublishedAt()).isNull();
        assertThat(event.orderId()).isEqualTo(paid.getId());
        assertThat(event.customerEmail()).isEqualTo("customer@example.com");
        assertThat(event.customerFirstName()).isEqualTo("John");
        assertThat(event.totalPriceCents()).isEqualTo(2598L);
        assertThat(event.items()).hasSize(1);
        assertThat(event.items().getFirst().productName()).isEqualTo("Coffee beans");
        assertThat(event.items().getFirst().quantity()).isEqualTo(2);
        assertThat(event.items().getFirst().unitPriceCents()).isEqualTo(1299L);
        assertThat(event.items().getFirst().totalPriceCents()).isEqualTo(2598L);
        // PostgreSQL rounds timestamp values to microsecond precision.
        assertThat(event.occurredAt()).isCloseTo(entry.getCreatedAt(), within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(paid.getOrderItems()).hasSize(1); // Also usable after the transaction ends.
        verifyNoInteractions(publisher);
    }

    @Test
    void repeatedCompletionKeepsTheOriginalConfirmation() {
        Order pending = pendingOrder();
        complete(pending);
        var original = outbox.findById(pending.getId()).orElseThrow();

        Order paid = complete(pending);

        var repeated = outbox.findById(paid.getId()).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getOrderItems()).hasSize(1);
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(repeated.getPayload()).isEqualTo(original.getPayload());
        assertThat(repeated.getCreatedAt()).isEqualTo(original.getCreatedAt());
        verifyNoInteractions(publisher);
    }

    @Test
    void repeatedCompletionDoesNotMakeAPublishedEventPendingAgain() throws Exception {
        Order pending = pendingOrder();
        complete(pending);
        doReturn(CompletableFuture.completedFuture(null)).when(publisher).publishOrderConfirmed(any());
        outboxPublisher.publish(pending.getId());
        var original = outbox.findById(pending.getId()).orElseThrow();

        complete(pending);

        var repeated = outbox.findById(pending.getId()).orElseThrow();
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(repeated.getPayload()).isEqualTo(original.getPayload());
        assertThat(repeated.getPublishedAt()).isEqualTo(original.getPublishedAt()).isNotNull();
        verify(publisher, times(1)).publishOrderConfirmed(any());
    }

    @Test
    void failedOutboxInsertRollsBackPaidStatus() {
        Order pending = pendingOrder();
        jdbc.execute("alter table order_confirmation_outbox add constraint reject_insert check (false)");
        try {
            assertThatThrownBy(() -> complete(pending)).isInstanceOf(RuntimeException.class);
            assertThat(orders.findById(pending.getId()).orElseThrow().getStatus())
                    .isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(outbox.findById(pending.getId())).isEmpty();
        } finally {
            jdbc.execute("alter table order_confirmation_outbox drop constraint reject_insert");
        }
    }

    @Test
    void marksPublishedOnlyAfterAcknowledgementAndSkipsAlreadyPublishedRows() throws Exception {
        Order paid = complete(pendingOrder());
        var acknowledgement = new CompletableFuture<SendResult<String, Object>>();
        var started = new CountDownLatch(1);
        doAnswer(call -> { started.countDown(); return acknowledgement; })
                .when(publisher).publishOrderConfirmed(any());

        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var publishing = executor.submit(() -> { outboxPublisher.publish(paid.getId()); return null; });
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(outbox.findById(paid.getId()).orElseThrow().getPublishedAt()).isNull();
            acknowledgement.complete(null);
            publishing.get(3, TimeUnit.SECONDS);
        } finally {
            acknowledgement.complete(null);
        }
        assertThat(outbox.findById(paid.getId()).orElseThrow().getPublishedAt()).isNotNull();
        outboxPublisher.publish(paid.getId());
        verify(publisher, times(1)).publishOrderConfirmed(any());
    }

    @Test
    void failedDeliveryRemainsPendingAndRetriesTheSameSavedEvent() throws Exception {
        Order paid = complete(pendingOrder());
        OrderConfirmedEvent saved = mapper.readValue(outbox.findById(paid.getId()).orElseThrow().getPayload(),
                OrderConfirmedEvent.class);
        doReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
                .when(publisher).publishOrderConfirmed(any());

        assertThatThrownBy(() -> outboxPublisher.publish(paid.getId())).isInstanceOf(ExecutionException.class);
        assertThat(outbox.findById(paid.getId()).orElseThrow().getPublishedAt()).isNull();
        assertThat(orders.findById(paid.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);

        doReturn(CompletableFuture.completedFuture(null)).when(publisher).publishOrderConfirmed(any());
        outboxPublisher.publish(paid.getId());
        verify(publisher, times(2)).publishOrderConfirmed(saved);
        assertThat(outbox.findById(paid.getId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    void acknowledgementFollowedByDatabaseFailureAllowsRedelivery() throws Exception {
        Order paid = complete(pendingOrder());
        doReturn(CompletableFuture.completedFuture(null)).when(publisher).publishOrderConfirmed(any());
        jdbc.execute("alter table order_confirmation_outbox add constraint reject_published check (published_at is null)");
        try {
            assertThatThrownBy(() -> outboxPublisher.publish(paid.getId())).isInstanceOf(RuntimeException.class);
            assertThat(outbox.findById(paid.getId()).orElseThrow().getPublishedAt()).isNull();
        } finally {
            jdbc.execute("alter table order_confirmation_outbox drop constraint reject_published");
        }
        outboxPublisher.publish(paid.getId());
        verify(publisher, times(2)).publishOrderConfirmed(any());
    }

    @Test
    void concurrentPublisherSkipsTheLockedRow() throws Exception {
        Order paid = complete(pendingOrder());
        var acknowledgement = new CompletableFuture<SendResult<String, Object>>();
        var started = new CountDownLatch(1);
        doAnswer(call -> { started.countDown(); return acknowledgement; })
                .when(publisher).publishOrderConfirmed(any());

        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { outboxPublisher.publish(paid.getId()); return null; });
            try {
                assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> { outboxPublisher.publish(paid.getId()); return null; });
                second.get(2, TimeUnit.SECONDS);
                verify(publisher, times(1)).publishOrderConfirmed(any());
            } finally {
                acknowledgement.complete(null);
            }
            first.get(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void successfulPurchaseCommitsStockAndQueuesConfirmationWithoutKafka() {
        Order paid = purchase(PaymentStatus.SUCCEEDED);

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getOrderItems()).hasSize(1);
        assertThat(outbox.findById(paid.getId())).isPresent();
        verify(catalog).commitStock(paid.getStockReservationId());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void declinedPaymentStillReleasesStockAndDoesNotQueueConfirmation() {
        Order failed = purchase(PaymentStatus.FAILED);

        assertThat(failed.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(outbox.findById(failed.getId())).isEmpty();
        verify(catalog).releaseStock(failed.getStockReservationId());
        verify(catalog, never()).commitStock(any());
        verifyNoInteractions(publisher);
    }

    private Order purchase(PaymentStatus paymentStatus) {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var customer = new CustomerResponse(customerId, "John", "Doe", null,
                "customer@example.com", null, true, null, null);
        when(customers.getMe("Bearer test-token")).thenReturn(customer);
        when(catalog.reserveStock(any())).thenAnswer(call -> {
            StockReservationRequest request = call.getArgument(0);
            return new StockReservationResponse(request.reservationId(), StockReservationStatus.RESERVED,
                    List.of(new StockReservationItemResponse(productId, UUID.randomUUID(),
                            "Coffee beans", 1299L, 2, 2598L)));
        });
        when(payments.createPayment(any()))
                .thenReturn(new PaymentResultResponse(UUID.randomUUID(), paymentStatus, "Test payment"));
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("test-customer").build();
        return purchaseService.purchase(jwt, new PurchaseRequest(
                List.of(new PurchaseItemRequest(productId, 2)), PaymentMethod.CARD));
    }

    private Order complete(Order pending) {
        var customer = new CustomerResponse(pending.getCustomerId(), "John", "Doe", null,
                "customer@example.com", null, true, null, null);
        return completion.markPaidAndQueueConfirmation(pending.getId(), customer);
    }

    private Order pendingOrder() {
        Order order = Order.builder().customerId(UUID.randomUUID())
                .stockReservationId(UUID.randomUUID()).paymentId(UUID.randomUUID())
                .status(OrderStatus.PENDING_PAYMENT).build();
        order.addItem(OrderItem.builder().productId(UUID.randomUUID()).merchantId(UUID.randomUUID())
                .productNameSnapshot("Coffee beans").merchantNameSnapshot("Coffee shop")
                .priceSnapshotCents(1299L).quantity(2).totalPriceSnapshotCents(2598L).build());
        order.recalculateTotalPrice();
        return orders.saveAndFlush(order);
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }
    }
}
