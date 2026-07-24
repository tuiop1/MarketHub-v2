package dev.tuiop.commonevents;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        UUID customerId,
        String customerEmail,
        String customerFirstName,
        Long totalPriceCents,
        List<OrderConfirmedItemSnapshot> items,
        Instant occurredAt
) {
}
