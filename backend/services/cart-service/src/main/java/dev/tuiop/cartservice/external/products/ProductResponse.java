package dev.tuiop.cartservice.external.products;



import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
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
