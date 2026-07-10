package dev.tuiop.cartservice.carts.mapper;


import dev.tuiop.cartservice.carts.dto.CartItemResponse;
import dev.tuiop.cartservice.carts.item.CartItem;
import dev.tuiop.cartservice.products.ProductResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    default CartItemResponse toCartItemResponse(CartItem cartItem, ProductResponse product) {
        Long totalPriceCents = product.priceCents() == null
                ? null
                : Math.multiplyExact(product.priceCents(), cartItem.getQuantity());

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getCart().getId(),
                cartItem.getProductId(),
                product.name(),
                product.priceCents(),
                cartItem.getQuantity(),
                totalPriceCents,
                product.active(),
                product.stockQuantity()
        );
    }
}
