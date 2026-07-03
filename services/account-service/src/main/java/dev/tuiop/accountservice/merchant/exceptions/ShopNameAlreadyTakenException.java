package dev.tuiop.accountservice.merchant.exceptions;


public class ShopNameAlreadyTakenException extends BusinessException {
    public ShopNameAlreadyTakenException(String shopName) {
        super("SHOP_NAME_ALREADY_TAKEN", "Shop name: " + shopName + " is already taken", 409);
    }
}
