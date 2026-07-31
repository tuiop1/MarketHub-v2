package dev.tuiop.paymentservice.payments.dto;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;

import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        UUID customerId,
        Long amountCents,
        PaymentMethod paymentMethod
) {
}
