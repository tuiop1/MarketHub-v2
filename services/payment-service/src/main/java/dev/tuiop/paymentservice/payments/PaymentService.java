package dev.tuiop.paymentservice.payments;


import dev.tuiop.paymentservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.paymentservice.payments.dto.CreatePaymentRequest;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepository paymentRepository;




    @Transactional
    // simulate behavior, if paid with card or google pay - succeeded, if paid with qr - failed
    public Payment createPayment(CreatePaymentRequest request){
        Optional<Payment> payment = paymentRepository.findByOrderId(request.orderId());

        if(payment.isPresent()){

            return payment.get();
        }


        PaymentMethod paymentMethod = request.paymentMethod();
        PaymentStatus paymentStatus;


        // qr payments are not supported
        if(paymentMethod ==  PaymentMethod.QR){
            paymentStatus = PaymentStatus.FAILED;

        }
        else{
            paymentStatus = PaymentStatus.SUCCEEDED;
        }

        Payment toSave = Payment.builder()
                .amountCents(request.amountCents())
                .method(paymentMethod)
                .status(paymentStatus)
                .orderId(request.orderId())
                .customerId(request.customerId())
                .build();

        return paymentRepository.save(toSave);



    }

    @Transactional
    public Payment cancelOrRefundByPaymentId(UUID paymentId){

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException(Payment.class, paymentId));

        payment.cancelOrRefund();
        return payment;

    }
}
