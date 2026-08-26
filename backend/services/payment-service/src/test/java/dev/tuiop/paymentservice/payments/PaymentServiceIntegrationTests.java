package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
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
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void clearDatabase() {
        paymentRepository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"CARD", "GOOGLE_PAY"})
    void successfulPaymentFlowIsIdempotentAndRefundable(PaymentMethod paymentMethod) throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest(orderId, customerId, 4_200L, paymentMethod)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        Payment created = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(created.getCustomerId()).isEqualTo(customerId);
        assertThat(created.getAmountCents()).isEqualTo(4_200L);
        assertThat(created.getMethod()).isEqualTo(paymentMethod);

        mockMvc.perform(post("/internal/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest(orderId, UUID.randomUUID(), 99_999L, PaymentMethod.QR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(created.getId().toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        assertThat(paymentRepository.count()).isOne();

        refund(created.getId(), PaymentStatus.REFUNDED);
        refund(created.getId(), PaymentStatus.REFUNDED);

        assertThat(paymentRepository.findById(created.getId()))
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void failedQrPaymentRemainsFailedWhenRefundIsRequested() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/internal/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest(orderId, UUID.randomUUID(), 2_500L, PaymentMethod.QR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        Payment failed = paymentRepository.findByOrderId(orderId).orElseThrow();
        refund(failed.getId(), PaymentStatus.FAILED);

        assertThat(paymentRepository.findById(failed.getId()))
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.FAILED);
    }

    private void refund(UUID paymentId, PaymentStatus expectedStatus) throws Exception {
        mockMvc.perform(post("/internal/v1/payments/{paymentId}/cancel-or-refund", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value(expectedStatus.name()));
    }

    private String paymentRequest(
            UUID orderId,
            UUID customerId,
            long amountCents,
            PaymentMethod paymentMethod
    ) {
        return """
                {
                  "orderId": "%s",
                  "customerId": "%s",
                  "amountCents": %d,
                  "paymentMethod": "%s"
                }
                """.formatted(orderId, customerId, amountCents, paymentMethod);
    }
}
