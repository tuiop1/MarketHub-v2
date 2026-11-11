package dev.tuiop.orderservice.orders.enums;

public enum OrderStatus {
    PENDING_RESERVATION,
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    COMPENSATION_FAILED
}
