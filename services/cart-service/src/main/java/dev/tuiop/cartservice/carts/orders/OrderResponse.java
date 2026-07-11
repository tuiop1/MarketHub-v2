package dev.tuiop.cartservice.carts.orders;



import dev.tuiop.cartservice.carts.orders.enums.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        Long totalPriceCents,
        List<OrderItemResponse> items,
        Instant createdAt


)


{
}
