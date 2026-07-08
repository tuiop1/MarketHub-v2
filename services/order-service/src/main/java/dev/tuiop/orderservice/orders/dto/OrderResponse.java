package dev.tuiop.orderservice.orders.dto;

import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.enums.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

        UUID id,
        OrderStatus status,
        Long totalPriceCents,
        List<OrderItemResponse> items,
        Instant createdAt,
        PaymentStatus paymentStatus


)


{
}
