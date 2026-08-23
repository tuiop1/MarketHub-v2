package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.dto.CreatePaymentRequest;
import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldReturnExistingPaymentForSameOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amountCents(2_500L)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCEEDED)
                .build();
        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                UUID.randomUUID(),
                9_999L,
                PaymentMethod.QR
        );

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        // Act
        Payment result = paymentService.createPayment(request);

        // Assert
        assertSame(existingPayment, result);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldCreateFailedPaymentForQrMethod() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                customerId,
                2_500L,
                PaymentMethod.QR
        );

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payment result = paymentService.createPayment(request);

        // Assert
        assertEquals(orderId, result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals(2_500L, result.getAmountCents());
        assertEquals(PaymentMethod.QR, result.getMethod());
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentRepository).save(result);
    }
}
