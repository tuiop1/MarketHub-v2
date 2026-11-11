package dev.tuiop.orderservice.orders.outbox;

import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.orderservice.kafka.OrderNotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class OrderConfirmationOutboxPublisher {
    private final OrderConfirmationOutboxRepository outboxRepository;
    private final OrderNotificationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // Keep the row locked during the bounded send to coordinate multiple service instances.
    // Checked send failures must also roll back and leave the row pending.
    @Transactional(rollbackFor = Exception.class)
    public void publish(UUID orderId) throws InterruptedException, ExecutionException, TimeoutException {
        var pending = outboxRepository.findPendingForUpdate(orderId);
        if (pending.isEmpty()) return;

        var entry = pending.get();
        var event = objectMapper.readValue(entry.getPayload(), OrderConfirmedEvent.class);
        eventPublisher.publishOrderConfirmed(event).get(5, TimeUnit.SECONDS);
        entry.markPublished();
    }
}
