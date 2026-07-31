package dev.tuiop.cartservice.carts.mapper;

import dev.tuiop.cartservice.carts.Cart;
import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.dto.CartResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = CartItemMapper.class)
public interface CartMapper {

    default CartResponse toCartResponse(Cart cart, List<CartItemResponse> cartItems) {
        long totalPriceCents = cartItems.stream()
                .map(CartItemResponse::totalPriceCents)
                .filter(total -> total != null)
                .mapToLong(Long::longValue)
                .sum();

        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                totalPriceCents,
                cartItems
        );
    }
}
