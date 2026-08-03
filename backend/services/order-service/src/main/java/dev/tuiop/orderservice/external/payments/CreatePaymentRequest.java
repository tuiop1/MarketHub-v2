package dev.tuiop.orderservice.external.payments;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;
@Builder
public record CreatePaymentRequest(
        @NotNull
        UUID orderId,
        @NotNull
        UUID customerId,
        @NotNull
        @Min(1)
        Long amountCents,
        @NotNull
        PaymentMethod paymentMethod
) {
}
