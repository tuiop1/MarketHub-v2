package dev.tuiop.accountservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.NotificationTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCustomerRegistered(CustomerRegisteredEvent event){
        kafkaTemplate.send(
                NotificationTopics.CUSTOMER_REGISTERED,
                event.customerId().toString(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish customer registration event: customerId={}, topic={}",
                        event.customerId(),
                        NotificationTopics.CUSTOMER_REGISTERED,
                        exception
                );
            }
        });
    }


    public void publishMerchantRegistered(MerchantRegisteredEvent event){
        kafkaTemplate.send(
                NotificationTopics.MERCHANT_REGISTERED,
                event.merchantId().toString(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish merchant registration event: merchantId={}, topic={}",
                        event.merchantId(),
                        NotificationTopics.MERCHANT_REGISTERED,
                        exception
                );
            }
        });
    }
}
