package dev.tuiop.catalogservice.products.reservations.mapper;

import dev.tuiop.catalogservice.products.reservations.StockReservation;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StockReservationItemMapper.class)
public interface StockReservationMapper {

    @Mapping(target = "reservationId", source = "id")
    StockReservationResponse toResponse(StockReservation reservation);
}
