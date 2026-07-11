package dev.tuiop.orderservice.products;

import java.util.UUID;

public record StockReservationRequest(
        UUID reservationId,
        List<StockReservationItemRequest> items
) {
}
