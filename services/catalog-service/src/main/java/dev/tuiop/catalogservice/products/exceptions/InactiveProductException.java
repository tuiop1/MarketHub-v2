package dev.tuiop.catalogservice.products.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

public class InactiveProductException extends BusinessException {
    public InactiveProductException(String name) {
        super("INACTIVE_PRODUCT", "Product \"" + name + "\" is inactive", 409);
    }
}
