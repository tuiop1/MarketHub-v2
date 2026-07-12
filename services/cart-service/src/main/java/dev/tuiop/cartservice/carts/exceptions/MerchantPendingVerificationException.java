package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class MerchantPendingVerificationException extends BusinessException {
    public MerchantPendingVerificationException(String shopName) {
        super("MERCHANT_PENDING_VERIFICATION", "Shop \"" + shopName + "\" is pending verification", 409);
    }
}
