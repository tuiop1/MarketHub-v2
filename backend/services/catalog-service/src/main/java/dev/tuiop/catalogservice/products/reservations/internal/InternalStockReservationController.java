package dev.tuiop.catalogservice.products.reservations.internal;

import dev.tuiop.catalogservice.products.reservations.StockReservationService;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationRequest;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationResponse;
import dev.tuiop.catalogservice.products.reservations.mapper.StockReservationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/catalog/stock-reservations")
@RequiredArgsConstructor
public class InternalStockReservationController {

    private final StockReservationService stockReservationService;
    private final StockReservationMapper stockReservationMapper;

    @PostMapping
    public ResponseEntity<StockReservationResponse> reserveStock(
            @Valid @RequestBody StockReservationRequest request
    ) {
        return ResponseEntity.ok(stockReservationMapper.toResponse(stockReservationService.reserveStock(request)));
    }

    @PostMapping("/{reservationId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseReservation(@PathVariable UUID reservationId) {
        stockReservationService.releaseReservation(reservationId);
    }

    @PostMapping("/{reservationId}/commit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void commitReservation(@PathVariable UUID reservationId) {
        stockReservationService.commitReservation(reservationId);
    }
}
