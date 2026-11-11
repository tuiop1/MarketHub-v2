package dev.tuiop.orderservice.orders.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmationOutboxWorker {
    private final OrderConfirmationOutboxRepository outboxRepository;
    private final OrderConfirmationOutboxPublisher outboxPublisher;

    @Scheduled(fixedDelayString = "${orders.confirmation-outbox.poll-delay-ms:5000}")
    public void publishPending() {
        for (var orderId : outboxRepository.findPendingOrderIds(PageRequest.of(0, 20))) {
            try {
                outboxPublisher.publish(orderId);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                log.warn("Order confirmation remains pending; will retry: orderId={}", orderId, exception);
            }
        }
    }
}
