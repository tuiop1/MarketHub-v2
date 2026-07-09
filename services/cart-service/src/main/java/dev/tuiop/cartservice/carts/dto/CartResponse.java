package dev.tuiop.cartservice.carts.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        Long totalPriceCents,
        List<CartItemResponse> cartItems
) {
}
