package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.external.customers.AccountCustomerClient;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
import dev.tuiop.orderservice.external.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.external.payments.PaymentResultResponse;
import dev.tuiop.orderservice.external.payments.PaymentStatus;
import dev.tuiop.orderservice.external.payments.client.PaymentServiceClient;
import dev.tuiop.orderservice.external.payments.exceptions.PaymentServiceException;
import dev.tuiop.orderservice.external.products.CatalogStockReservationClient;
import dev.tuiop.orderservice.external.products.StockReservationItemResponse;
import dev.tuiop.orderservice.external.products.StockReservationRequest;
import dev.tuiop.orderservice.external.products.StockReservationResponse;
import dev.tuiop.orderservice.external.products.StockReservationStatus;
import dev.tuiop.orderservice.external.products.exceptions.CatalogServiceException;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.outbox.OrderPaymentCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPurchaseSagaServiceTest {
    @Mock OrderRepository orders;
    @Mock AccountCustomerClient customers;
    @Mock CatalogStockReservationClient catalog;
    @Mock PaymentServiceClient payments;
    @Mock OrderPaymentCompletionService completion;
    private OrderPurchaseSagaService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private Order savedOrder;

    @BeforeEach
    void createService() {
        service = new OrderPurchaseSagaService(orders, new OrderReservationService(orders),
                customers, catalog, payments, completion);
    }

    @Test
    void lostPaymentResponseCancelsByOrderIdAfterReleasingStock() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(payments.createPayment(any())).thenThrow(new ResourceAccessException("Payment response lost"));
        when(payments.cancelOrRefundByOrderId(orderId)).thenReturn(payment(PaymentStatus.REFUNDED));

        assertThatThrownBy(this::purchase).isInstanceOf(PaymentServiceException.class);

        var calls = inOrder(catalog, payments);
        calls.verify(catalog).reserveStock(any());
        calls.verify(payments, times(3)).createPayment(any());
        calls.verify(catalog).releaseStock(savedOrder.getStockReservationId());
        calls.verify(payments).cancelOrRefundByOrderId(orderId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(savedOrder.getPaymentId()).isEqualTo(paymentId);
        verify(payments, times(3)).createPayment(any());
        verify(payments, times(1)).cancelOrRefundByOrderId(orderId);
        verify(payments, never()).cancelOrRefund(any());
        verifyNoInteractions(completion);
    }

    @Test
    void missingPaymentAfterTimeoutKeepsCleanupUnconfirmed() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(payments.createPayment(any())).thenThrow(new ResourceAccessException("Payment response lost"));
        when(payments.cancelOrRefundByOrderId(orderId)).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(this::purchase).isInstanceOf(PaymentServiceException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        assertThat(savedOrder.getFailureReason()).contains("payment cancellation or refund could not be confirmed");
        verify(payments, times(3)).createPayment(any());
        verify(payments, times(1)).cancelOrRefundByOrderId(orderId);
        verifyNoInteractions(completion);
    }

    @Test
    void lostCommitResponseDoesNotRefundWhenReleaseCannotBeConfirmed() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        doThrow(new ResourceAccessException("Commit response lost")).when(catalog).commitStock(any());
        doThrow(new IllegalStateException("Committed reservation cannot be released"))
                .when(catalog).releaseStock(any());

        assertThatThrownBy(this::purchase).isInstanceOf(CatalogServiceException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(catalog, times(3)).commitStock(any());
        verify(catalog, times(1)).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
        verify(payments, never()).cancelOrRefundByOrderId(any());
        verifyNoInteractions(completion);
    }

    @Test
    void failedCommitCancelsWhenReleaseAndRefundSucceed() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        doThrow(new ResourceAccessException("Commit request failed")).when(catalog).commitStock(any());
        when(payments.cancelOrRefund(paymentId)).thenReturn(payment(PaymentStatus.REFUNDED));

        assertThatThrownBy(this::purchase).isInstanceOf(CatalogServiceException.class);

        var calls = inOrder(catalog, payments);
        calls.verify(catalog).reserveStock(any());
        calls.verify(payments).createPayment(any());
        calls.verify(catalog, times(3)).commitStock(any());
        calls.verify(catalog).releaseStock(savedOrder.getStockReservationId());
        calls.verify(payments).cancelOrRefund(paymentId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(catalog, times(3)).commitStock(any());
        verify(catalog, times(1)).releaseStock(any());
        verify(payments, times(1)).cancelOrRefund(paymentId);
    }

    @Test
    void failedPaidTransactionAfterConfirmedCommitDoesNotRefundOrRelease() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any()))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThatThrownBy(this::purchase).isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(catalog, times(1)).commitStock(any());
        verify(completion, times(3)).markPaidAndQueueConfirmation(eq(orderId), any());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
        verify(payments, never()).cancelOrRefundByOrderId(any());
    }

    @Test
    void failedReleaseAfterDeclinedPaymentStopsAfterThreeAttempts() {
        preparePurchase(PaymentStatus.FAILED);
        doThrow(new ResourceAccessException("Release response lost")).when(catalog).releaseStock(any());

        assertThatThrownBy(this::purchase).isInstanceOf(CatalogServiceException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(catalog, times(3)).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
        verify(payments, never()).cancelOrRefundByOrderId(any());
    }

    @Test
    void failedRefundStopsAfterThreeAttemptsAndRemainsUnconfirmed() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        doThrow(new ResourceAccessException("Commit failed")).when(catalog).commitStock(any());
        when(payments.cancelOrRefund(paymentId)).thenThrow(new ResourceAccessException("Refund response lost"));

        assertThatThrownBy(this::purchase).isInstanceOf(CatalogServiceException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(payments, times(3)).cancelOrRefund(paymentId);
        verify(catalog, times(1)).releaseStock(any());
    }

    @Test
    void pendingPaymentIsCancelledWithoutRepeatingThePaymentRequest() {
        preparePurchase(PaymentStatus.PENDING);
        when(payments.cancelOrRefund(paymentId)).thenReturn(payment(PaymentStatus.CANCELLED));

        assertThatThrownBy(this::purchase).isInstanceOf(IllegalArgumentException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(payments, times(1)).createPayment(any());
        verify(payments, times(1)).cancelOrRefund(paymentId);
        verify(catalog, never()).commitStock(any());
    }

    @Test
    void paidOrderIsPreservedIfOnlyTheDatabaseAcknowledgementWasLost() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any())).thenAnswer(call -> {
            savedOrder.markPaid(); // Simulate a committed transaction with a lost acknowledgement.
            throw new IllegalStateException("Database acknowledgement lost");
        });

        assertThatThrownBy(this::purchase).isInstanceOf(IllegalStateException.class);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
        verify(payments, never()).cancelOrRefundByOrderId(any());
    }

    @Test
    void paymentRetryUsesTheSameRequestAndContinuesCheckout() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(payments.createPayment(any())).thenThrow(new ResourceAccessException("Payment response lost"))
                .thenReturn(payment(PaymentStatus.SUCCEEDED));
        completeSuccessfully();

        Order paid = purchase();

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        var requests = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(payments, times(2)).createPayment(requests.capture());
        assertThat(requests.getAllValues().get(1)).isSameAs(requests.getAllValues().getFirst());
        assertThat(requests.getValue().orderId()).isEqualTo(orderId);
        verify(catalog, times(1)).commitStock(any());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
    }

    @Test
    void reservationRetryUsesTheSameReservationId() {
        preparePurchase(PaymentStatus.SUCCEEDED, true);
        completeSuccessfully();

        Order paid = purchase();

        var requests = ArgumentCaptor.forClass(StockReservationRequest.class);
        verify(catalog, times(2)).reserveStock(requests.capture());
        assertThat(requests.getAllValues().get(1)).isSameAs(requests.getAllValues().getFirst());
        assertThat(requests.getValue().reservationId()).isEqualTo(paid.getStockReservationId());
        verify(payments, times(1)).createPayment(any());
    }

    @Test
    void serverErrorDuringCommitIsRetriedWithoutRepeatingPayment() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        doThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .doNothing().when(catalog).commitStock(any());
        completeSuccessfully();

        assertThat(purchase().getStatus()).isEqualTo(OrderStatus.PAID);

        verify(catalog, times(2)).commitStock(any());
        verify(payments, times(1)).createPayment(any());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
    }

    @Test
    void temporaryPaidTransactionFailureRetriesOnlyTheFinalStep() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any()))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"))
                .thenAnswer(call -> { savedOrder.markPaid(); return savedOrder; });

        assertThat(purchase().getStatus()).isEqualTo(OrderStatus.PAID);

        verify(completion, times(2)).markPaidAndQueueConfirmation(eq(orderId), any());
        verify(payments, times(1)).createPayment(any());
        verify(catalog, times(1)).commitStock(any());
        verify(catalog, never()).releaseStock(any());
    }

    @Test
    void lostDatabaseAcknowledgementCanReturnTheAlreadyPaidOrderOnRetry() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any())).thenAnswer(call -> {
            savedOrder.markPaid();
            throw new DataAccessResourceFailureException("Database acknowledgement lost");
        }).thenAnswer(call -> savedOrder);

        assertThat(purchase().getStatus()).isEqualTo(OrderStatus.PAID);

        verify(completion, times(2)).markPaidAndQueueConfirmation(eq(orderId), any());
        verify(catalog, times(1)).commitStock(any());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
    }

    @Test
    void badPaymentRequestIsNotRetried() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(payments.createPayment(any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        when(payments.cancelOrRefundByOrderId(orderId)).thenReturn(payment(PaymentStatus.FAILED));

        assertThatThrownBy(this::purchase).isInstanceOf(PaymentServiceException.class);

        verify(payments, times(1)).createPayment(any());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void permanentDatabaseConstraintFailureIsNotRetried() {
        preparePurchase(PaymentStatus.SUCCEEDED);
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any()))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        assertThatThrownBy(this::purchase).isInstanceOf(DataIntegrityViolationException.class);

        verify(completion, times(1)).markPaidAndQueueConfirmation(eq(orderId), any());
        verify(catalog, never()).releaseStock(any());
        verify(payments, never()).cancelOrRefund(any());
    }

    private void completeSuccessfully() {
        when(completion.markPaidAndQueueConfirmation(eq(orderId), any())).thenAnswer(call -> {
            savedOrder.markPaid();
            return savedOrder;
        });
    }

    private void preparePurchase(PaymentStatus status) {
        preparePurchase(status, false);
    }

    private void preparePurchase(PaymentStatus status, boolean loseFirstReservationResponse) {
        UUID customerId = UUID.randomUUID();
        when(customers.getMe("Bearer test-token")).thenReturn(new CustomerResponse(customerId,
                "John", "Doe", null, "customer@example.com", null, true, null, null));
        var reservationCall = when(catalog.reserveStock(any()));
        if (loseFirstReservationResponse) {
            reservationCall = reservationCall.thenThrow(new ResourceAccessException("Reservation response lost"));
        }
        reservationCall.thenAnswer(call -> reservation(call.getArgument(0)));
        when(orders.save(any())).thenAnswer(call -> {
            Order order = call.getArgument(0);
            if (order.getId() == null) ReflectionTestUtils.setField(order, "id", orderId);
            savedOrder = order;
            return order;
        });
        when(orders.findById(orderId)).thenAnswer(call -> Optional.of(savedOrder));
        when(payments.createPayment(any())).thenReturn(payment(status));
    }

    private PaymentResultResponse payment(PaymentStatus status) {
        return new PaymentResultResponse(paymentId, status, "Test payment");
    }

    private StockReservationResponse reservation(StockReservationRequest request) {
        return new StockReservationResponse(request.reservationId(), StockReservationStatus.RESERVED,
                List.of(new StockReservationItemResponse(productId, UUID.randomUUID(),
                        "Coffee beans", 1299L, 2, 2598L)));
    }

    private Order purchase() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("test-customer").build();
        return service.purchase(jwt, new PurchaseRequest(
                List.of(new PurchaseItemRequest(productId, 2)), PaymentMethod.CARD));
    }
}
