package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.dto.CreatePaymentRequest;
import dev.tuiop.paymentservice.payments.dto.PaymentResultResponse;
import dev.tuiop.paymentservice.payments.mapper.PaymentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResultResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.ok(paymentMapper.toResultResponse(payment));
         }
    @PostMapping
    public PaymentResultResponse cancelOrRefund(
            @PathVariable UUID paymentId
    ){
        Payment payment = paymentService.cancelOrRefund(paymentId);
        return paymentMapper.toResultResponse(payment);
    }



}
