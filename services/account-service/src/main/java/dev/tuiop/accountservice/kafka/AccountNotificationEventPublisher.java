package dev.tuiop.accountservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.NotificationTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountNotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCustomerRegistered(CustomerRegisteredEvent event){
        kafkaTemplate.send(
                NotificationTopics.CUSTOMER_REGISTERED,
                event.customerId().toString(),
                event
        );
    }


    public void publishMerchantRegistered(MerchantRegisteredEvent event){
        kafkaTemplate.send(
                NotificationTopics.MERCHANT_REGISTERED,
                event.merchantId().toString(),
                event
        );
    }
}
