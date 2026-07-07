package dev.tuiop.orderservice.orders.exceptions;

import dev.tuiop.orderservice.common.exceptions.BusinessException;

public class OwnProductPurchaseException extends BusinessException {
    public OwnProductPurchaseException(String message) {
        super("OWN_PRODUCT_PURCHASE", message, 409);
    }
}
