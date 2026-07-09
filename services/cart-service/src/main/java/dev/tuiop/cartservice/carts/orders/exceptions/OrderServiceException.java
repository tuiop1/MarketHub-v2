package dev.tuiop.cartservice.carts.orders.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class OrderServiceException extends BusinessException {

    private OrderServiceException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }

    public static OrderServiceException unavailable(Throwable cause) {
        return new OrderServiceException(
                "ORDER_SERVICE_UNAVAILABLE",
                "Order service is currently unavailable",
                503,
                cause
        );
    }

    public static OrderServiceException unauthorized(Throwable cause) {
        return new OrderServiceException(
                "ORDER_SERVICE_AUTHORIZATION_FAILED",
                "Cart service is not authorized to call order service",
                503,
                cause
        );
    }

    public static OrderServiceException rejected(Throwable cause) {
        return new OrderServiceException(
                "ORDER_SERVICE_REJECTED_REQUEST",
                "Order service rejected the purchase request",
                409,
                cause
        );
    }
}
