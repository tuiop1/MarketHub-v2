package dev.tuiop.orderservice.orders.exceptions;

import dev.tuiop.orderservice.common.exceptions.BusinessException;
import dev.tuiop.orderservice.orders.enums.OrderStatus;

import java.util.UUID;

public class OrderNotPendingPaymentException extends BusinessException {

    public OrderNotPendingPaymentException(UUID orderId, OrderStatus status) {
        super(
                "ORDER_NOT_PENDING_PAYMENT",
                "Order " + orderId + " is not pending payment. Current status: " + status,
                409
        );
    }
}
