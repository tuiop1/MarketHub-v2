package dev.tuiop.paymentservice.payments.internal;

import dev.tuiop.paymentservice.payments.Payment;
import dev.tuiop.paymentservice.payments.PaymentService;
import dev.tuiop.paymentservice.payments.dto.CreatePaymentRequest;
import dev.tuiop.paymentservice.payments.dto.PaymentResultResponse;
import dev.tuiop.paymentservice.payments.mapper.PaymentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResultResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.ok(paymentMapper.toResultResponse(payment));
    }

    @PostMapping("/{paymentId}/cancel-or-refund")
    public PaymentResultResponse cancelOrRefund(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.cancelOrRefundByPaymentId(paymentId);
        return paymentMapper.toResultResponse(payment);
    }
}
