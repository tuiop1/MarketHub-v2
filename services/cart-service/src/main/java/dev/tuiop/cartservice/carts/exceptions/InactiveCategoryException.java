package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class InactiveCategoryException extends BusinessException {
    public InactiveCategoryException(String name) {
        super("INACTIVE_CATEGORY", "Category \"" + name + "\" is inactive", 409);
    }
}
