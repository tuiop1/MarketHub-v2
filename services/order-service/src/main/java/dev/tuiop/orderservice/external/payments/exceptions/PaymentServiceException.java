package dev.tuiop.orderservice.external.payments.exceptions;

import dev.tuiop.orderservice.common.exceptions.TechnicalException;

public class PaymentServiceException extends TechnicalException {

    private PaymentServiceException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }

    public static PaymentServiceException unavailable(Throwable cause) {
        return new PaymentServiceException(
                "PAYMENT_SERVICE_UNAVAILABLE",
                "Payment service is currently unavailable",
                503,
                cause
        );
    }

    public static PaymentServiceException unauthorized(Throwable cause) {
        return new PaymentServiceException(
                "PAYMENT_SERVICE_AUTHORIZATION_FAILED",
                "Order service is not authorized to call payment service",
                503,
                cause
        );
    }

    public static PaymentServiceException rejected(Throwable cause) {
        return new PaymentServiceException(
                "PAYMENT_SERVICE_REJECTED_REQUEST",
                "Payment service rejected the payment request",
                409,
                cause
        );
    }
}
