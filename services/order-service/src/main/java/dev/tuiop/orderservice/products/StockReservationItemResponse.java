package dev.tuiop.orderservice.products;

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
