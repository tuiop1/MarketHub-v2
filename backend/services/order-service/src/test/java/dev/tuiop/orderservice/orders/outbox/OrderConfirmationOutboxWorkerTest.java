package dev.tuiop.orderservice.orders.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConfirmationOutboxWorkerTest {
    @Mock OrderConfirmationOutboxRepository outbox;
    @Mock OrderConfirmationOutboxPublisher outboxPublisher;
    @InjectMocks OrderConfirmationOutboxWorker worker;

    @Test
    void continuesWithOtherOrdersWhenOneSendFails() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(outbox.findPendingOrderIds(any())).thenReturn(List.of(first, second));
        doThrow(new ExecutionException(new IllegalStateException("Kafka unavailable"))).when(outboxPublisher).publish(first);

        worker.publishPending();

        verify(outboxPublisher).publish(first);
        verify(outboxPublisher).publish(second);
    }
}
