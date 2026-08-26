package dev.tuiop.paymentservice.payments.internal;

import dev.tuiop.paymentservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.paymentservice.payments.Payment;
import dev.tuiop.paymentservice.payments.PaymentService;
import dev.tuiop.paymentservice.payments.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalPaymentControllerMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @Test
    void invalidPaymentRequestReturnsValidationErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/internal/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountCents": 0,
                                  "paymentMethod": "CARD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.customerId").exists())
                .andExpect(jsonPath("$.errors.amountCents").exists());

        verifyNoInteractions(paymentService, paymentMapper);
    }

    @Test
    void refundForUnknownPaymentReturnsNotFoundError() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.cancelOrRefundByPaymentId(paymentId))
                .thenThrow(new ResourceNotFoundException(Payment.class, paymentId));

        mockMvc.perform(post("/internal/v1/payments/{paymentId}/cancel-or-refund", paymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(
                        "/internal/v1/payments/" + paymentId + "/cancel-or-refund"
                ));

        verifyNoInteractions(paymentMapper);
    }
}
