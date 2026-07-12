package dev.tuiop.orderservice.external.payments;

import java.util.UUID;

public record PaymentResultResponse(
        UUID paymentId,
        PaymentStatus status,
        String info
) {
}
