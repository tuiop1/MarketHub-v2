package dev.tuiop.accountservice.merchant.exceptions;

public class SuspendedMerchantException extends BusinessException {
    public SuspendedMerchantException(String shopName ) {
        super("MERCHANT_SUSPENDED", "Shop \"" + shopName + "\" is inactive", 409);
    }
}
