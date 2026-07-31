package dev.tuiop.catalogservice.products.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

import java.util.UUID;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int requestedQuantity, int availableQuantity) {
        super(
                "INSUFFICIENT_STOCK",
                "Not enough stock for product '%s'. Requested: %d, available: %d"
                        .formatted(productName, requestedQuantity, availableQuantity),
                409
        );
    }

    public InsufficientStockException(UUID productId, int availableQuantity, int requestedQuantity) {
        super(
                "INSUFFICIENT_STOCK",
                "Not enough stock for product '%s'. Requested: %d, available: %d"
                        .formatted(productId, requestedQuantity, availableQuantity),
                409
        );
    }
}
