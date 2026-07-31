package dev.tuiop.catalogservice.products.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

import java.util.UUID;

public class ProductNotAvailableException extends BusinessException {

    public ProductNotAvailableException(UUID productId) {
        super(
                "PRODUCT_NOT_AVAILABLE",
                "Product is not available: " + productId,
                409
        );
    }
}
