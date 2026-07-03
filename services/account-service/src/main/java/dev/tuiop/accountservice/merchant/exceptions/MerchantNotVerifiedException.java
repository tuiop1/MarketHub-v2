package dev.tuiop.accountservice.merchant.exceptions;

public class MerchantNotVerifiedException extends BusinessException {
    public MerchantNotVerifiedException() {
        super("MERCHANT_NOT_VERIFIED", "Merchant must be verified to manage products", 409);
    }
}
