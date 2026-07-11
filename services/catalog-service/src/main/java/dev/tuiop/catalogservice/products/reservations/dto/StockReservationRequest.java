package dev.tuiop.catalogservice.products.reservations.dto;


import java.util.List;
import java.util.UUID;

public record StockReservationRequest(
        UUID reservationId,
        List<StockReservationItemRequest> items
) {
}
