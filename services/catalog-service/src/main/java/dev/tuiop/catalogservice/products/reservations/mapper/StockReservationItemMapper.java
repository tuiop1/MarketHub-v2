package dev.tuiop.catalogservice.products.reservations.mapper;

import dev.tuiop.catalogservice.products.reservations.StockReservationItem;
import dev.tuiop.catalogservice.products.reservations.dto.StockReservationItemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockReservationItemMapper {

    StockReservationItemResponse toResponse(StockReservationItem item);
}
