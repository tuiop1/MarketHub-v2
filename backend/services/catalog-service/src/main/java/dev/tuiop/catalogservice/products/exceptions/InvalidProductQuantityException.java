package dev.tuiop.catalogservice.products.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

public class InvalidProductQuantityException extends BusinessException {

    public InvalidProductQuantityException(int quantity) {
        super(
                "INVALID_PRODUCT_QUANTITY",
                "Product quantity must be positive. Quantity: " + quantity,
                400
        );
    }
}
