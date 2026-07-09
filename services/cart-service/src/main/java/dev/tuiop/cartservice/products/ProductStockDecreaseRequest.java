package dev.tuiop.cartservice.products;

import java.util.UUID;

public record ProductStockDecreaseRequest(
        UUID productId,
        Integer quantity
) {
}
