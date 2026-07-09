package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class RejectedMerchantException extends BusinessException {
    public RejectedMerchantException(String shopName) {
        super("MERCHANT_REJECTED", "Shop \"" + shopName + "\" has been rejected", 409);
    }
}
