package dev.tuiop.orderservice.payments.client;

import dev.tuiop.orderservice.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.payments.PaymentResultResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

@HttpExchange("/api/v1/payments")
public interface PaymentServiceClient {

    @PostExchange
    PaymentResultResponse createPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreatePaymentRequest request
    );

    @PostExchange("/{paymentId}/cancel-or-refund")
    PaymentResultResponse cancelOrRefund(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID paymentId
    );

}
