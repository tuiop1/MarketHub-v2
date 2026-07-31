package dev.tuiop.orderservice.external.products;

import lombok.Builder;

import java.util.List;
import java.util.UUID;
@Builder
public record StockReservationRequest(
        UUID reservationId,
        List<StockReservationItemRequest> items
) {
}
