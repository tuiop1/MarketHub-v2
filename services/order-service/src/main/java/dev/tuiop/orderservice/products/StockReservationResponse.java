package dev.tuiop.orderservice.products;

import java.util.List;
import java.util.UUID;

public record StockReservationResponse(
        UUID reservationId,
        StockReservationStatus status,
        List<StockReservationItemResponse> items
) {
}
