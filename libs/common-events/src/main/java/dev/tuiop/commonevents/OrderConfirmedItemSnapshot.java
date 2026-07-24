package dev.tuiop.commonevents;

import java.util.UUID;

public record OrderConfirmedItemSnapshot(
        UUID productId,
        String productName,
        Integer quantity,
        Long unitPriceCents,
        Long totalPriceCents

) {
}
