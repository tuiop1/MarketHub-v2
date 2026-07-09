package dev.tuiop.cartservice.carts.orders;

import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID merchantId,
        String productNameSnapshot,
        String merchantNameSnapshot,
        Long priceSnapshotCents,
        Integer quantity,
        Long totalPriceSnapshotCents
) {
}

