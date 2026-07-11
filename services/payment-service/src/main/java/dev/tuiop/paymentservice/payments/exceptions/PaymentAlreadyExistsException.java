package dev.tuiop.paymentservice.payments.exceptions;

import dev.tuiop.paymentservice.common.exceptions.BusinessException;

import java.util.UUID;

public class PaymentAlreadyExistsException extends BusinessException {

    public PaymentAlreadyExistsException(UUID orderId) {
        super(
                "PAYMENT_ALREADY_EXISTS",
                "Payment already exists for orderId: " + orderId,
                409
        );
    }
}
