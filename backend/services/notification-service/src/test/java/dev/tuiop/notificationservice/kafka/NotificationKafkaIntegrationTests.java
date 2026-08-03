package dev.tuiop.notificationservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.commonevents.OrderConfirmedItemSnapshot;
import dev.tuiop.notificationservice.email.EmailSender;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(properties = {
        "sendgrid.api-key=disabled",
        "sendgrid.from-email=noreply@markethub.test",
        "sendgrid.from-name=MarketHub Test"
})
@Import(NotificationKafkaIntegrationTests.KafkaProducerTestConfiguration.class)
class NotificationKafkaIntegrationTests {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void resetEmailSender() {
        reset(emailSender);
    }

    @Test
    void customerRegistrationEventIsConsumedFromKafka() throws Exception {
        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
                UUID.randomUUID(),
                "customer@example.com",
                "Taylor",
                Instant.now()
        );

        send(NotificationTopics.CUSTOMER_REGISTERED, event.customerId().toString(), event);

        verify(emailSender, timeout(15_000)).sendHtml(
                eq(event.email()),
                eq("Thank you for registration in MarketHub!"),
                contains("Taylor")
        );
    }

    @Test
    void merchantRegistrationEventIsConsumedFromKafka() throws Exception {
        MerchantRegisteredEvent event = new MerchantRegisteredEvent(
                UUID.randomUUID(),
                "merchant@example.com",
                "Coffee House",
                Instant.now()
        );

        send(NotificationTopics.MERCHANT_REGISTERED, event.merchantId().toString(), event);

        verify(emailSender, timeout(15_000)).sendHtml(
                eq(event.email()),
                eq("Thank you for registration in MarketHub as Merchant!"),
                contains("Coffee House")
        );
    }

    @Test
    void orderConfirmationEventIsConsumedFromKafka() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                orderId,
                UUID.randomUUID(),
                "customer@example.com",
                "Taylor",
                2_500L,
                List.of(new OrderConfirmedItemSnapshot(
                        UUID.randomUUID(), "Coffee", 2, 1_250L, 2_500L
                )),
                Instant.now()
        );

        send(NotificationTopics.ORDER_CONFIRMED, orderId.toString(), event);

        verify(emailSender, timeout(15_000)).sendHtml(
                eq(event.customerEmail()),
                eq("MarketHub order confirmation"),
                contains("25.00 USD")
        );
    }

    private void send(String topic, String key, Object event) throws Exception {
        kafkaTemplate.send(topic, key, event).get(10, TimeUnit.SECONDS);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaProducerTestConfiguration {

        @Bean
        ProducerFactory<String, Object> testProducerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
        ) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(properties);
        }

        @Bean
        KafkaTemplate<String, Object> testKafkaTemplate(
                ProducerFactory<String, Object> testProducerFactory
        ) {
            return new KafkaTemplate<>(testProducerFactory);
        }
    }
}
