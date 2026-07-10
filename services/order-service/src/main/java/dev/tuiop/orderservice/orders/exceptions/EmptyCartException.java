package dev.tuiop.orderservice.orders.exceptions;

import dev.tuiop.orderservice.common.exceptions.BusinessException;

public class EmptyCartException extends BusinessException {
    public EmptyCartException() {
        super("EMPTY_CART", "The cart is empty", 409);
    }
}
