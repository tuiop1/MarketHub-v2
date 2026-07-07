package dev.tuiop.orderservice.orders.mapper;


import com.tuiop.markethub.orders.OrderItem;
import com.tuiop.markethub.orders.dto.OrderItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "merchantId", source = "merchant.id")
    OrderItemResponse toOrderItemResponse(OrderItem order);




}
