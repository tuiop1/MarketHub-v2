package dev.tuiop.orderservice.orders;

import dev.tuiop.orderservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.orderservice.external.customers.AccountCustomerClient;
import dev.tuiop.orderservice.external.customers.CustomerResponse;
import dev.tuiop.orderservice.external.customers.exceptions.AccountServiceException;
import dev.tuiop.orderservice.orders.dto.PurchaseItemRequest;
import dev.tuiop.orderservice.orders.dto.PurchaseRequest;
import dev.tuiop.orderservice.orders.enums.OrderStatus;
import dev.tuiop.orderservice.orders.outbox.OrderPaymentCompletionService;
import dev.tuiop.orderservice.external.payments.CreatePaymentRequest;
import dev.tuiop.orderservice.external.payments.PaymentMethod;
import dev.tuiop.orderservice.external.payments.PaymentResultResponse;
import dev.tuiop.orderservice.external.payments.PaymentStatus;
import dev.tuiop.orderservice.external.payments.client.PaymentServiceClient;
import dev.tuiop.orderservice.external.payments.exceptions.PaymentServiceException;
import dev.tuiop.orderservice.external.products.CatalogStockReservationClient;
import dev.tuiop.orderservice.external.products.StockReservationItemRequest;
import dev.tuiop.orderservice.external.products.StockReservationRequest;
import dev.tuiop.orderservice.external.products.StockReservationResponse;
import dev.tuiop.orderservice.external.products.exceptions.CatalogServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPurchaseSagaService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final OrderRepository orderRepository;
    private final OrderReservationService orderReservationService;
    private final AccountCustomerClient accountCustomerClient;
    private final CatalogStockReservationClient catalogStockReservationClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderPaymentCompletionService paymentCompletionService;

    public Order purchase(Jwt jwt, PurchaseRequest purchaseRequest) {
        UUID stockReservationId = UUID.randomUUID();

        Map<UUID, Integer> quantitiesByProductId =
                mergeQuantitiesByProductId(purchaseRequest.items());

        CustomerResponse customerResponse = getMe(jwt);

        List<StockReservationItemRequest> items = quantitiesByProductId.keySet()
                .stream()
                .map(id -> new StockReservationItemRequest(id, quantitiesByProductId.get(id)))
                .toList();

        StockReservationRequest request = StockReservationRequest.builder()
                .reservationId(stockReservationId)
                .items(items)
                .build();

        Order order = orderReservationService.createPendingReservation(customerResponse.id(), stockReservationId);
        PaymentResultResponse payment = null;
        boolean paymentAttempted = false;
        boolean stockReleaseAttempted = false;
        boolean stockReleased = false;
        boolean stockCommitted = false;
        try {

            StockReservationResponse reservation = reserveStock(request);

            UUID reservedOrderId = order.getId();
            order = retry("save stock reservation snapshots",
                    () -> orderReservationService.recordReservation(
                            reservedOrderId, reservation.items(), quantitiesByProductId));

            paymentAttempted = true;
            payment = createPayment(order, purchaseRequest.paymentMethod());

            attachPayment(order.getId(), payment.paymentId());

            if (payment.status() == PaymentStatus.FAILED) {
                stockReleaseAttempted = true;
                releaseStockSafely(stockReservationId);
                stockReleased = true;

                Order failedOrder = markPaymentFailed(order.getId(), payment.info());

                log.info(
                        "Order payment failed: orderId={}, paymentId={}, stockReservationId={}",
                        failedOrder.getId(),
                        payment.paymentId(),
                        stockReservationId
                );

                return failedOrder;
            }

            if (payment.status() == PaymentStatus.SUCCEEDED) {

                commitStockSafely(stockReservationId);
                stockCommitted = true;

                // Saves the confirmation for the outbox publisher.
                UUID orderId = order.getId();
                Order paidOrder = retry("save paid order and confirmation",
                        () -> paymentCompletionService.markPaidAndQueueConfirmation(orderId, customerResponse));

                log.info(
                        "Order paid: orderId={}, paymentId={}, stockReservationId={}, totalPriceCents={}",
                        paidOrder.getId(),
                        payment.paymentId(),
                        stockReservationId,
                        paidOrder.getTotalPriceCents()
                );

                return paidOrder;

            }

            throw new IllegalArgumentException("Unsupported payment status: " + payment.status());
        } catch (Exception exception) {
            if (stockCommitted) {
                recordCompensationFailure(order, stockReservationId, exception,
                        "Stock was committed; automatic release and refund were skipped");
            } else if (stockReleaseAttempted && !stockReleased) {
                recordCompensationFailure(order, stockReservationId, exception,
                        "Stock release was attempted but could not be confirmed");
            } else {
                compensateAfterUnexpectedFailure(order, payment, stockReservationId,
                        paymentAttempted, stockReleased, exception);
            }
            throw exception;
        }
    }


    //compensation logic

    private void compensateAfterUnexpectedFailure(Order order, PaymentResultResponse payment,
                                                  UUID stockReservationId, boolean paymentAttempted,
                                                  boolean stockReleased, Exception originalException) {

        log.warn(
                "Purchase saga failed. Starting compensation: orderId={}, paymentId={}, stockReservationId={}",
                order != null ? order.getId() : null,
                payment != null ? payment.paymentId() : null,
                stockReservationId,
                originalException
        );

        // Catalog serializes release and commit on the reservation row. Only a
        // confirmed release makes it safe to continue with a payment refund.
        if (!stockReleased) {
            try {
                releaseStockSafely(stockReservationId);
            } catch (Exception cleanupException) {
                originalException.addSuppressed(cleanupException);
                recordCompensationFailure(order, stockReservationId, originalException,
                        "Stock release could not be confirmed; payment refund was skipped");
                return;
            }
        }

        if (paymentAttempted && !isPaymentSettledWithoutCharge(payment)) {
            try {
                // A lost createPayment response leaves payment null, but the
                // saved order ID still lets Payment Service find the charge.
                PaymentResultResponse result = retry("cancel or refund payment",
                        () -> payment != null && payment.paymentId() != null
                                ? paymentServiceClient.cancelOrRefund(payment.paymentId())
                                : paymentServiceClient.cancelOrRefundByOrderId(order.getId()));
                if (!isPaymentSettledWithoutCharge(result)) {
                    throw new IllegalStateException("Payment cancellation was not confirmed");
                }
                attachPayment(order.getId(), result.paymentId());
            } catch (Exception cleanupException) {
                originalException.addSuppressed(cleanupException);
                recordCompensationFailure(order, stockReservationId, originalException,
                        "Stock was released, but payment cancellation or refund could not be confirmed");
                return;
            }
        }
        if (order != null) {
            try {
                cancel(order.getId(), failureReason(originalException, "Stock and payment cleanup completed"));
                log.warn("Purchase cancelled after compensation: orderId={}, stockReservationId={}",
                        order.getId(), stockReservationId);
            } catch (Exception cleanupException) {
                originalException.addSuppressed(cleanupException);
                log.error("Could not save cancelled order: orderId={}", order.getId(), cleanupException);
            }
        }
    }

    private boolean isPaymentSettledWithoutCharge(PaymentResultResponse payment) {
        return payment != null && (payment.status() == PaymentStatus.FAILED
                || payment.status() == PaymentStatus.REFUNDED
                || payment.status() == PaymentStatus.CANCELLED);
    }

    private void recordCompensationFailure(Order order, UUID stockReservationId,
                                           Exception originalException, String details) {
        log.error("Purchase cleanup incomplete: orderId={}, stockReservationId={}, details={}",
                order != null ? order.getId() : null, stockReservationId, details, originalException);
        if (order != null) {
            try {
                markCompensationFailed(order.getId(), failureReason(originalException, details));
            } catch (Exception persistenceException) {
                originalException.addSuppressed(persistenceException);
                log.error("Could not record incomplete cleanup: orderId={}", order.getId(), persistenceException);
            }
        }
    }

    private String failureReason(Exception exception, String details) {
        String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage() + "; " + details;
        return reason.substring(0, Math.min(reason.length(), 1000));
    }

    private Order attachPayment(UUID orderId, UUID paymentId) {
        Order order = getOrder(orderId);
        order.attachPayment(paymentId);
        return orderRepository.save(order);
    }


    public Order markPaymentFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.markPaymentFailed(reason);
        return orderRepository.save(order);
    }

    public Order cancel(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        return orderRepository.save(order);
    }

    public void markCompensationFailed(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        // A database commit can succeed even if its acknowledgement is lost.
        // Preserve PAID and its confirmation if a fresh read finds them saved.
        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Preserving paid order after purchase error: orderId={}", orderId);
            return;
        }
        order.markCompensationFailed(reason);
        orderRepository.save(order);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
    }

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<PurchaseItemRequest> items) {
        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            quantitiesByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }

    private CustomerResponse getMe(Jwt jwt) {
        try {
            return accountCustomerClient.getMe(authorizationHeader(jwt));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(CustomerResponse.class, "current");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting current customer", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting current customer", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }


    private PaymentResultResponse createPayment(Order order, PaymentMethod paymentMethod) {
        try {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(order.getId())
                    .customerId(order.getCustomerId())
                    .amountCents(order.getTotalPriceCents())
                    .paymentMethod(paymentMethod)
                    .build();
            return retry("create payment", () -> paymentServiceClient.createPayment(request));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Payment service authorization failed while creating payment", exception);
            throw PaymentServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Payment service rejected create payment request", exception);
            throw PaymentServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Payment service request failed while creating payment", exception);
            throw PaymentServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Payment service client failed while creating payment", exception);
            throw PaymentServiceException.unavailable(exception);
        }
    }

    private String authorizationHeader(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }

    private StockReservationResponse reserveStock(StockReservationRequest request) {
        try {
            return retry("reserve stock", () -> catalogStockReservationClient.reserveStock(request));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while reserving stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock reservation request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while reserving stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while reserving stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private void releaseStockSafely(UUID stockReservationId) {
        try {
            retry("release stock", () -> {
                catalogStockReservationClient.releaseStock(stockReservationId);
                return null;
            });
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while releasing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock release request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while releasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while releasing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    private void commitStockSafely(UUID stockReservationId) {
        try {
            retry("commit stock", () -> {
                catalogStockReservationClient.commitStock(stockReservationId);
                return null;
            });
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Catalog service authorization failed while committing stock", exception);
            throw CatalogServiceException.unauthorized(exception);
        } catch (HttpClientErrorException exception) {
            log.warn("Catalog service rejected stock commit request", exception);
            throw CatalogServiceException.rejected(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Catalog service request failed while committing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Catalog service client failed while committing stock", exception);
            throw CatalogServiceException.unavailable(exception);
        }
    }

    // Retry individual steps with the same IDs, never the whole purchase method.
    private <T> T retry(String operation, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException exception) {
                if (attempt == MAX_ATTEMPTS || !isRetryable(exception)) {
                    throw exception;
                }
                log.warn("Purchase step failed; retrying: operation={}, attempt={}/{}, error={}",
                        operation, attempt, MAX_ATTEMPTS, exception.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    exception.addSuppressed(interruption);
                    throw exception;
                }
            }
        }
    }

    private boolean isRetryable(RuntimeException exception) {
        if (exception instanceof TransactionSystemException transactionException
                && transactionException.contains(DataIntegrityViolationException.class)) {
            return false;
        }
        return exception instanceof ResourceAccessException
                || exception instanceof HttpServerErrorException
                || exception instanceof TransientDataAccessException
                || exception instanceof DataAccessResourceFailureException
                || exception instanceof CannotCreateTransactionException
                || exception instanceof TransactionSystemException;
    }

}
