package dev.tuiop.orderservice.kafka;

import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationEventPublisher {


    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void publishOrderConfirmed(OrderConfirmedEvent event){
        kafkaTemplate.send(
                NotificationTopics.ORDER_CONFIRMED,
                event.orderId().toString(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish order confirmation event: orderId={}, topic={}",
                        event.orderId(),
                        NotificationTopics.ORDER_CONFIRMED,
                        exception
                );
            }
        });
    }
}
