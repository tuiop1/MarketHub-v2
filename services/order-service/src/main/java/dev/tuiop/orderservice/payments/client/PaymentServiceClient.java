package dev.tuiop.orderservice.payments.client;

import dev.tuiop.orderservice.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.payments.PaymentResultResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/v1/payments")
public interface PaymentServiceClient {

    @PostExchange
    PaymentResultResponse createPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreatePaymentRequest request
    );

}
