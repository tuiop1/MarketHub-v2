package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class InactiveMerchantException extends BusinessException {
    public InactiveMerchantException(String shopName) {
        super("INACTIVE_MERCHANT", "Shop \"" + shopName + "\" is inactive", 409);
    }
}
