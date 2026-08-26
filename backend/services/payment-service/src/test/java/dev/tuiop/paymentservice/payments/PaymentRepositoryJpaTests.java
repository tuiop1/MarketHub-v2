package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
class PaymentRepositoryJpaTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.14-alpine3.24")
            .withDatabaseName("payment_repository_test")
            .withUsername("markethub")
            .withPassword("markethub");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void savesAndFindsPaymentByOrderId() {
        UUID orderId = UUID.randomUUID();
        Payment saved = paymentRepository.saveAndFlush(payment(orderId));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(paymentRepository.findByOrderId(orderId))
                .containsSame(saved);
    }

    @Test
    void databaseRejectsTwoPaymentsForTheSameOrder() {
        UUID orderId = UUID.randomUUID();
        paymentRepository.saveAndFlush(payment(orderId));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment(orderId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Payment payment(UUID orderId) {
        return Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amountCents(2_500L)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCEEDED)
                .build();
    }
}
