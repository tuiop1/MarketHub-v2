package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.dto.CreatePaymentRequest;
import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class PaymentServiceIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("payment_test")
            .withUsername("markethub")
            .withPassword("markethub");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void clearDatabase() {
        paymentRepository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"CARD", "GOOGLE_PAY"})
    void supportedPaymentMethodsSucceed(PaymentMethod method) {
        Payment payment = paymentService.createPayment(request(UUID.randomUUID(), UUID.randomUUID(), 2_500L, method));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentRepository.findById(payment.getId()))
                .get()
                .satisfies(persisted -> {
                    assertThat(persisted.getId()).isEqualTo(payment.getId());
                    assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
                });
    }

    @Test
    void qrPaymentFailsAndIsPersisted() {
        Payment payment = paymentService.createPayment(request(
                UUID.randomUUID(), UUID.randomUUID(), 2_500L, PaymentMethod.QR
        ));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(paymentRepository.findById(payment.getId()))
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void duplicateRequestForOrderReturnsOriginalPayment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreatePaymentRequest request = request(orderId, customerId, 4_200L, PaymentMethod.CARD);

        Payment first = paymentService.createPayment(request);
        Payment duplicate = paymentService.createPayment(request);

        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(paymentRepository.count()).isOne();
    }

    @Test
    void duplicateOrderCannotChangeOriginalPaymentDetails() {
        UUID orderId = UUID.randomUUID();
        UUID originalCustomerId = UUID.randomUUID();
        Payment original = paymentService.createPayment(request(
                orderId, originalCustomerId, 4_200L, PaymentMethod.CARD
        ));

        Payment duplicate = paymentService.createPayment(request(
                orderId, UUID.randomUUID(), 99_999L, PaymentMethod.QR
        ));

        assertThat(duplicate.getId()).isEqualTo(original.getId());
        assertThat(duplicate.getCustomerId()).isEqualTo(originalCustomerId);
        assertThat(duplicate.getAmountCents()).isEqualTo(4_200L);
        assertThat(duplicate.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(duplicate.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void successfulPaymentCanBeRefundedIdempotently() {
        Payment payment = paymentService.createPayment(request(
                UUID.randomUUID(), UUID.randomUUID(), 4_200L, PaymentMethod.CARD
        ));

        Payment firstRefund = paymentService.cancelOrRefundByPaymentId(payment.getId());
        Payment duplicateRefund = paymentService.cancelOrRefundByPaymentId(payment.getId());

        assertThat(firstRefund.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(duplicateRefund.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentRepository.findById(payment.getId()))
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void failedPaymentIsNotChangedByRefundRequest() {
        Payment payment = paymentService.createPayment(request(
                UUID.randomUUID(), UUID.randomUUID(), 4_200L, PaymentMethod.QR
        ));

        Payment result = paymentService.cancelOrRefundByPaymentId(payment.getId());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    private CreatePaymentRequest request(
            UUID orderId,
            UUID customerId,
            long amountCents,
            PaymentMethod paymentMethod
    ) {
        return new CreatePaymentRequest(orderId, customerId, amountCents, paymentMethod);
    }
}
