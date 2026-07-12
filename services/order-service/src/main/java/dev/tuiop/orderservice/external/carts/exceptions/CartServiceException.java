package dev.tuiop.orderservice.external.carts.exceptions;

import dev.tuiop.orderservice.common.exceptions.BusinessException;

public class CartServiceException extends BusinessException {

    private CartServiceException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }

    public static CartServiceException unavailable(Throwable cause) {
        return new CartServiceException(
                "CART_SERVICE_UNAVAILABLE",
                "Cart service is currently unavailable",
                503,
                cause
        );
    }

    public static CartServiceException unauthorized(Throwable cause) {
        return new CartServiceException(
                "CART_SERVICE_AUTHORIZATION_FAILED",
                "Order service is not authorized to call cart service",
                503,
                cause
        );
    }

    public static CartServiceException rejected(Throwable cause) {
        return new CartServiceException(
                "CART_SERVICE_REJECTED_REQUEST",
                "Cart service rejected the request",
                409,
                cause
        );
    }
}
