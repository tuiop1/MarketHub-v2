package dev.tuiop.paymentservice.payments.dto;

import dev.tuiop.paymentservice.payments.enums.PaymentStatus;

import java.util.UUID;

public record PaymentResultResponse(
        UUID paymentId,
        PaymentStatus status,
        String info
) {
}
