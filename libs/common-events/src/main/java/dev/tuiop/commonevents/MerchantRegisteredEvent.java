package dev.tuiop.commonevents;

import java.time.Instant;
import java.util.UUID;

public record MerchantRegisteredEvent(
        UUID merchantId,
        String email,
        String shopName,
        Instant occurredAt
) {
}
