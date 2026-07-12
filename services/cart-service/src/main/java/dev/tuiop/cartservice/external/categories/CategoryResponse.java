package dev.tuiop.cartservice.external.categories;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        Boolean active,
        Instant createdAt
) {
}
