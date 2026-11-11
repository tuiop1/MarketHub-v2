package dev.tuiop.orderservice.kafka;

import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class OrderNotificationEventPublisher {


    private final KafkaTemplate<String, Object> kafkaTemplate;


    public CompletableFuture<SendResult<String, Object>> publishOrderConfirmed(OrderConfirmedEvent event){
        return kafkaTemplate.send(
                NotificationTopics.ORDER_CONFIRMED,
                event.orderId().toString(),
                event
        );
    }
}
