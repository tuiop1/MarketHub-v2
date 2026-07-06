package dev.tuiop.catalogservice.client.merchants.exceptions;

import dev.tuiop.catalogservice.client.merchants.MerchantStatus;
import dev.tuiop.catalogservice.common.exceptions.BusinessException;

public class MerchantInvalidStatusException extends BusinessException {
    public MerchantInvalidStatusException(MerchantStatus status) {
        super("MERCHANT_INVALID_STATUS", "Status " + status.toString() + " is invalid for this operation", 409);
    }

}
