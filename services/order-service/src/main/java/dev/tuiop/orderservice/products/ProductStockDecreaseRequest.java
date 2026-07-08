package dev.tuiop.orderservice.products;

import java.util.UUID;

public record ProductStockDecreaseRequest(
        UUID productId,
        Integer quantity
) {
}
