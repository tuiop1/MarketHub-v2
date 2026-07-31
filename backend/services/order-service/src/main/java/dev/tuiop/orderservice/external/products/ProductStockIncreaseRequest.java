package dev.tuiop.orderservice.external.products;

import java.util.UUID;

public record ProductStockIncreaseRequest(
        UUID productId,
        Integer quantity
) {
}
