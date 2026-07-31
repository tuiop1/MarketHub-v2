package dev.tuiop.notificationservice.kafka;

import dev.tuiop.commonevents.CustomerRegisteredEvent;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import dev.tuiop.commonevents.NotificationTopics;
import dev.tuiop.commonevents.OrderConfirmedEvent;
import dev.tuiop.notificationservice.email.EmailSender;
import dev.tuiop.notificationservice.email.EmailTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {


    private final EmailSender emailSender;

    private final EmailTemplateFactory templateFactory;

    @KafkaListener(
            topics = NotificationTopics.CUSTOMER_REGISTERED,
            groupId = "notification-service"
    )
    public void consumeCustomerRegisteredEvent(CustomerRegisteredEvent event){


        emailSender.sendHtml(event.email(), "Thank you for registration in MarketHub!", templateFactory.customerRegistered(event));

        log.info("Customer registration email sent: customerId={}", event.customerId());

    }

    @KafkaListener(
            topics = NotificationTopics.MERCHANT_REGISTERED,
            groupId = "notification-service"
    )
    public void consumeMerchantRegisteredEvent(MerchantRegisteredEvent event){

        emailSender.sendHtml(
                event.email(),
                "Thank you for registration in MarketHub as Merchant!",
                templateFactory.merchantRegistered(event)
        );

        log.info("Merchant registration email sent: merchantId={}", event.merchantId());

    }



    @KafkaListener(
            topics = NotificationTopics.ORDER_CONFIRMED,
            groupId = "notification-service"
    )
    public void consumeOrderConfirmedEvent(OrderConfirmedEvent event){
        emailSender.sendHtml(
                event.customerEmail(),
                "MarketHub order confirmation",
                templateFactory.orderPaid(event)
        );

        log.info(
                "Order confirmation email sent: orderId={}, customerId={}",
                event.orderId(),
                event.customerId()
        );


    }


}
