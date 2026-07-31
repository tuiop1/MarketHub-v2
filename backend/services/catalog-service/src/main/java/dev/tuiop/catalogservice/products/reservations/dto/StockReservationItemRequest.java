package dev.tuiop.catalogservice.products.reservations.dto;

import java.util.UUID;

public record StockReservationItemRequest(
        UUID productId,
        Integer quantity
) {
}
