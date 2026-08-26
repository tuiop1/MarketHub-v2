package dev.tuiop.notificationservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.notificationservice.email.EmailSender;
import dev.tuiop.notificationservice.email.EmailTemplateFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private EmailTemplateFactory templateFactory;

    @InjectMocks
    private NotificationEventConsumer eventConsumer;

    @Test
    void shouldSendWelcomeEmailForRegisteredCustomer() {
        // Arrange
        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
                UUID.randomUUID(),
                "john@gmail.com",
                "John",
                Instant.now()
        );
        String emailBody = "<h1>Welcome, John!</h1>";
        when(templateFactory.customerRegistered(event)).thenReturn(emailBody);

        // Act
        eventConsumer.consumeCustomerRegisteredEvent(event);

        // Assert
        verify(emailSender).sendHtml(
                "john@gmail.com",
                "Thank you for registration in MarketHub!",
                emailBody
        );
    }
}
