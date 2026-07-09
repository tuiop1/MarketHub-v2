package dev.tuiop.cartservice.carts.mapper;


import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.item.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartItemMapper {


    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "priceCents", ignore = true)
    @Mapping(target = "productActive", ignore = true)
    @Mapping(target = "stockQuantity", ignore = true)
    @Mapping(target = "totalPriceCents", ignore = true)
    CartItemResponse toCartItemResponse(CartItem cartItem);
}
