package dev.tuiop.paymentservice.payments.dto;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull UUID customerId,
        @NotNull @Positive Long amountCents,
        @NotNull PaymentMethod paymentMethod
) {
}
