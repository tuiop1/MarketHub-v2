package dev.tuiop.orderservice.products;

import java.util.UUID;

public record StockReservationItemRequest(
        UUID productId,
        Integer quantity
) {
}
