package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class SuspendedMerchantException extends BusinessException {
    public SuspendedMerchantException(String shopName) {
        super("MERCHANT_SUSPENDED", "Shop \"" + shopName + "\" is suspended", 409);
    }
}
