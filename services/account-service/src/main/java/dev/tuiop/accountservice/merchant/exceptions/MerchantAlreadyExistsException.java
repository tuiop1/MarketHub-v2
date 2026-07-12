package dev.tuiop.accountservice.merchant.exceptions;

import dev.tuiop.accountservice.common.exceptions.BusinessException;

public class MerchantAlreadyExistsException extends BusinessException {
    public MerchantAlreadyExistsException() {
        super("MERCHANT_ALREADY_EXISTS", "User is already merchant", 409);
    }
}
