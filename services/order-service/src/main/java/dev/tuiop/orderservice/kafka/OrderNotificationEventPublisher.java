package dev.tuiop.orderservice.kafka;

import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNotificationEventPublisher {


    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void publishOrderConfirmed(OrderConfirmedEvent event){
        kafkaTemplate.send(
                NotificationTopics.ORDER_CONFIRMED,
                event.orderId().toString(),
                event
        );
    }
}
