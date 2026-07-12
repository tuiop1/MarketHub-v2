package dev.tuiop.orderservice.external.products;

import java.util.UUID;

public record ProductStockDecreaseRequest(
        UUID productId,
        Integer quantity
) {
}
