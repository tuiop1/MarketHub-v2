package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class InactiveProductException extends BusinessException {
    public InactiveProductException(String name) {
        super("INACTIVE_PRODUCT", "Product \"" + name + "\" is inactive", 409);
    }
}
