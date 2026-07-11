package dev.tuiop.orderservice.payments;

import lombok.Builder;

import java.util.UUID;
@Builder
public record CreatePaymentRequest(
        UUID orderId,
        UUID customerId,
        Long amountCents,
        PaymentMethod paymentMethod
) {
}
