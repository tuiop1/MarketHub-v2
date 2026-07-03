package dev.tuiop.accountservice.merchant.exceptions;

public class MerchantAlreadyExistsException extends BusinessException {
    public MerchantAlreadyExistsException() {
        super("MERCHANT_ALREADY_EXISTS", "User is already merchant", 409);
    }
}
