package dev.tuiop.catalogservice.products.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID merchantId,
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
