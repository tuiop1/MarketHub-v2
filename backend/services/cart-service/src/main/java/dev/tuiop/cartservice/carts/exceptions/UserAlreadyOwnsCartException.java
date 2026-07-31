package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class UserAlreadyOwnsCartException extends BusinessException {
    public UserAlreadyOwnsCartException() {
        super("USER_ALREADY_OWNS_CART", "User already has a cart", 409);
    }
}
