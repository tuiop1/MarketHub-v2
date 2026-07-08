package dev.tuiop.catalogservice.products.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductPurchaseResponse(
        UUID id,
        UUID merchantId,
        String merchantName,
        UUID categoryId,
        String name,
        String description,
        Long priceCents,
        Integer stockQuantity,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
