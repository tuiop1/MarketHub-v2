package dev.tuiop.cartservice.carts.mapper;

import dev.tuiop.cartservice.carts.Cart;
import dev.tuiop.cartservice.carts.dto.CartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CartItemMapper.class)
public interface CartMapper {

    @Mapping(target = "userId", source = "customerId")
    @Mapping(target = "cartItems", source = "cartItems")
    @Mapping(target = "totalPriceCents", ignore = true)
    CartResponse toCartResponse(Cart cart);
}
