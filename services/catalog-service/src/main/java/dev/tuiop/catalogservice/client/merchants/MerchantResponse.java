package dev.tuiop.catalogservice.client.merchants;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String shopName,
        String description,
        String email,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant verifiedAt
) {
}
