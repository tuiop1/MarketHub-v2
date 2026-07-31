package dev.tuiop.accountservice.customer.exceptions;

import dev.tuiop.accountservice.common.exceptions.BusinessException;

public class CustomerAlreadyExistsException extends BusinessException {
    public CustomerAlreadyExistsException() {
        super("CUSTOMER_ALREADY_EXISTS", "User is already customer", 409);
    }
}
