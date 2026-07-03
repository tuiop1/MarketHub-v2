package dev.tuiop.accountservice.merchant.exceptions;

import dev.tuiop.accountservice.common.exceptions.BusinessException;

public class SuspendedMerchantException extends BusinessException {
    public SuspendedMerchantException(String shopName ) {
        super("MERCHANT_SUSPENDED", "Shop \"" + shopName + "\" is inactive", 409);
    }
}
