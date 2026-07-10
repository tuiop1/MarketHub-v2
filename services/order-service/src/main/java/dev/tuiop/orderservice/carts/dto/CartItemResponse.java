package dev.tuiop.orderservice.carts.dto;


import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID cartId,
        UUID productId,
        String productName,
        Long priceCents,
        Integer quantity,
        Long totalPriceCents,
        Boolean productActive,
        Integer stockQuantity


) {
}
