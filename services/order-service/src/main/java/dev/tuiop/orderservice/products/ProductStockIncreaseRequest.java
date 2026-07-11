package dev.tuiop.orderservice.products;

import java.util.UUID;

public record ProductStockIncreaseRequest(
        UUID productId,
        Integer quantity
) {
}
