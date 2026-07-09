package dev.tuiop.cartservice.carts.orders;

import java.util.UUID;

public record PurchaseItemRequest(
        UUID productId,
        Integer quantity
) {
}
