package dev.tuiop.accountservice.merchant.exceptions;

import dev.tuiop.accountservice.common.exceptions.BusinessException;
import dev.tuiop.accountservice.merchant.MerchantStatus;

public class MerchantInvalidStatusTransitionException extends BusinessException {
    public MerchantInvalidStatusTransitionException(MerchantStatus actualStatus, MerchantStatus desiredStatus) {
        super("MERCHANT_INVALID_STATUS_TRANSITION", "Transition from " + actualStatus.name() + " to " + desiredStatus.name() + " is invalid", 409);
    }
}
