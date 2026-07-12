package dev.tuiop.cartservice.external.orders;

import java.util.UUID;

public record PurchaseItemRequest(
        UUID productId,
        Integer quantity
) {
}
