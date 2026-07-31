package dev.tuiop.orderservice.external.products;

import java.util.UUID;

public record StockReservationItemRequest(
        UUID productId,
        Integer quantity
) {
}
