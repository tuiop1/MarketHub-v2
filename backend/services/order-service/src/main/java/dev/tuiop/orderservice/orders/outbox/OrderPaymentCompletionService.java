package dev.tuiop.orderservice.orders.outbox;

import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.commonevents.OrderConfirmedItemSnapshot;
import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.orders.Order;
import dev.tuiop.orderservice.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentCompletionService {
    private final OrderRepository orderRepository;
    private final OrderConfirmationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // Called through a separate Spring bean so this transaction actually applies.
    @Transactional
    public Order markPaidAndQueueConfirmation(UUID orderId, CustomerResponse customer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
        order.markPaid();

        var items = order.getOrderItems().stream()
                .map(item -> new OrderConfirmedItemSnapshot(
                        item.getProductId(), item.getProductNameSnapshot(), item.getQuantity(),
                        item.getPriceSnapshotCents(), item.getTotalPriceSnapshotCents()))
                .toList();
        Instant now = Instant.now();
        var event = new OrderConfirmedEvent(order.getId(), order.getCustomerId(),
                customer.email(), customer.firstName(), order.getTotalPriceCents(), items, now);
        outboxRepository.save(new OrderConfirmationOutbox(
                orderId, objectMapper.writeValueAsString(event), now));

        // Hibernate saves the managed order at commit. Both changes commit or roll back together.
        return order;
    }
}
