package dev.tuiop.orderservice.external.products;

import java.util.UUID;

public record StockReservationItemResponse(
        UUID productId,
        UUID merchantId,
        String productName,
        Long unitPriceCents,
        Integer quantity,
        Long totalPriceCents
) {
}
