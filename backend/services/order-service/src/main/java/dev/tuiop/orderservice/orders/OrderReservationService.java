package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.external.products.StockReservationItemResponse;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderReservationService {
    private final OrderRepository orderRepository;

    // This transaction commits before the purchase method calls Catalog.
    @Transactional
    public Order createPendingReservation(UUID customerId, UUID reservationId) {
        return orderRepository.save(Order.builder()
                .customerId(customerId)
                .stockReservationId(reservationId)
                .status(OrderStatus.PENDING_RESERVATION)
                .totalPriceCents(0L)
                .build());
    }

    @Transactional
    public Order recordReservation(UUID orderId, List<StockReservationItemResponse> items,
                                   Map<UUID, Integer> quantitiesByProductId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
        // A retry after a lost database acknowledgement must not add items twice.
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.getOrderItems().size();
            return order;
        }
        order.markPendingPayment();
        for (StockReservationItemResponse item : items) {
            order.addItem(OrderItem.fromReservation(item, quantitiesByProductId.get(item.productId())));
        }
        order.recalculateTotalPrice();
        // Hibernate commits the snapshots, total, and new status together.
        return order;
    }
}
