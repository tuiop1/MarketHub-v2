package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class EmptyCartException extends BusinessException {
    public EmptyCartException() {
        super("EMPTY_CART", "The cart is empty", 409);
    }
}
