package dev.tuiop.accountservice.merchant.exceptions;

import dev.tuiop.accountservice.common.exceptions.BusinessException;

public class MerchantShopNameTakenException extends BusinessException {
    public MerchantShopNameTakenException(String shopName) {
        super("MERCHANT_SHOP_NAME_ALREADY_TAKEN", "Shop name: " + shopName + " is already taken", 409);
    }
}
