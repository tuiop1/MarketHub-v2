package dev.tuiop.cartservice.carts.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        Long totalPriceCents,
        List<CartItemResponse> cartItems
) {
}
