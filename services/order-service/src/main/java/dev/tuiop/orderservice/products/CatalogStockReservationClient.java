package dev.tuiop.orderservice.products;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

@HttpExchange("/api/v1/products/purchase/reservations")
public interface CatalogStockReservationClient {

    @PostExchange
    StockReservationResponse reserveStock(@RequestBody StockReservationRequest request);

    @PostExchange("/{reservationId}/release")
    void releaseStock(@PathVariable UUID reservationId);

    @PostExchange("/{reservationId}/commit")
    void commitStock(@PathVariable UUID reservationId);
}
