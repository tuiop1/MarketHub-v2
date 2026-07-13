package dev.tuiop.notificationservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {



    @KafkaListener(
            topics = NotificationTopics.CUSTOMER_REGISTERED,
            groupId = "notification-service"
    )
    public void consumeCustomerRegisteredEvent(CustomerRegisteredEvent event){

        log.info("consumed customer registered event: {} ", event);


    }

    @KafkaListener(
            topics = NotificationTopics.MERCHANT_REGISTERED,
            groupId = "notification-service"
    )
    public void consumeMerchantRegisteredEvent(MerchantRegisteredEvent event){

        log.info("consumed merchant registered event: {} ", event);


    }



    @KafkaListener(
            topics = NotificationTopics.ORDER_CONFIRMED,
            groupId = "notification-service"
    )
    public void consumeOrderConfirmedEvent(OrderConfirmedEvent event){

        log.info("consumed order confirmed event: {} ", event);


    }


}
