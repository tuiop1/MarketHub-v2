package dev.tuiop.orderservice.external.customers.exceptions;


import dev.tuiop.orderservice.common.exceptions.TechnicalException;

public class AccountServiceException extends TechnicalException {

    private AccountServiceException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }

    public static AccountServiceException unavailable(Throwable cause) {
        return new AccountServiceException(
                "ACCOUNT_SERVICE_UNAVAILABLE",
                "Account service is currently unavailable",
                503,
                cause
        );
    }

    public static AccountServiceException unauthorized(Throwable cause) {
        return new AccountServiceException(
                "ACCOUNT_SERVICE_AUTHORIZATION_FAILED",
                "Order service is not authorized to call account service",
                503,
                cause
        );
    }
}
