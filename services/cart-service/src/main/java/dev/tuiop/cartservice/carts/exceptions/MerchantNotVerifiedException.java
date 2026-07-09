package dev.tuiop.cartservice.carts.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class MerchantNotVerifiedException extends BusinessException {
    public MerchantNotVerifiedException() {
        super("MERCHANT_NOT_VERIFIED", "Merchant must be verified to add products to cart", 409);
    }

    public MerchantNotVerifiedException(String shopName) {
        super("MERCHANT_NOT_VERIFIED", "Shop \"" + shopName + "\" must be verified to add products to cart", 409);
    }
}
