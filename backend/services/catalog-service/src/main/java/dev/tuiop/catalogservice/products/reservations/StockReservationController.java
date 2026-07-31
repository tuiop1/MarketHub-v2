package dev.tuiop.catalogservice.products.reservations;

import dev.tuiop.catalogservice.products.reservations.dto.StockReservationRequest;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationResponse;
import dev.tuiop.catalogservice.products.reservations.mapper.StockReservationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/purchase/reservations")
public class StockReservationController {

    private final StockReservationService stockReservationService;
    private final StockReservationMapper stockReservationMapper;


    @PostMapping
    public ResponseEntity<StockReservationResponse> reserveStock(
            @Valid @RequestBody StockReservationRequest request
            ){
        return ResponseEntity.ok( stockReservationMapper.toResponse(stockReservationService.reserveStock(request)));
    }

    @PostMapping("/{reservationId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseReservation(
            @PathVariable UUID reservationId
    ) {
        stockReservationService.releaseReservation(reservationId);
    }

    @PostMapping("/{reservationId}/commit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void commitReservation(
            @PathVariable UUID reservationId
    ) {
        stockReservationService.commitReservation(reservationId);
    }

}
