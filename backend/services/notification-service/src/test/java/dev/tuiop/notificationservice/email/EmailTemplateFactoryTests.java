package dev.tuiop.notificationservice.email;

import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.commonevents.OrderConfirmedItemSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailTemplateFactoryTests {

    private final EmailTemplateFactory templateFactory = new EmailTemplateFactory();

    @Test
    void orderPaidFormatsEveryPriceInUsd() {
        OrderConfirmedItemSnapshot item = new OrderConfirmedItemSnapshot(
                UUID.randomUUID(),
                "Coffee",
                2,
                1_250L,
                2_500L
        );
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "customer@example.com",
                "Taylor",
                2_500L,
                List.of(item),
                Instant.now()
        );

        String email = templateFactory.orderPaid(event);

        assertEquals(3, occurrences(email, "USD"));

    }

    private int occurrences(String value, String searchTerm) {
        return (value.length() - value.replace(searchTerm, "").length()) / searchTerm.length();
    }
}
