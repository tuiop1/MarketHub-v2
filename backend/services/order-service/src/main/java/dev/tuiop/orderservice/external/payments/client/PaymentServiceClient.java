package dev.tuiop.orderservice.external.payments.client;

import dev.tuiop.orderservice.external.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.external.payments.PaymentResultResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

@HttpExchange("/internal/v1/payments")
public interface PaymentServiceClient {

    @PostExchange
    PaymentResultResponse createPayment(@RequestBody CreatePaymentRequest request);

    @PostExchange("/{paymentId}/cancel-or-refund")
    PaymentResultResponse cancelOrRefund(@PathVariable("paymentId") UUID paymentId);

}
