package dev.tuiop.orderservice.orders.mapper;

import dev.tuiop.orderservice.orders.Order;
import dev.tuiop.orderservice.orders.dto.OrderResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "items", source = "orderItems")
    OrderResponse toOrderResponse(Order order) ;
}
