package dev.tuiop.catalogservice.products.reservations.dto;

import dev.tuiop.catalogservice.products.reservations.StockReservationStatus;

import java.util.List;
import java.util.UUID;

public record StockReservationResponse(
        UUID reservationId,
        StockReservationStatus status,
        List<StockReservationItemResponse> items
) {
}
