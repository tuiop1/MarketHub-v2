package dev.tuiop.orderservice.orders.mapper;


import dev.tuiop.orderservice.orders.OrderItem;
import dev.tuiop.orderservice.orders.dto.OrderItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemResponse toOrderItemResponse(OrderItem order);




}
