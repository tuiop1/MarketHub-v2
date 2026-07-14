package dev.tuiop.notificationservice.email;

import dev.tuiop.notificationservice.common.exceptions.BusinessException;

public class EmailDeliveryException extends BusinessException {

    private static final String CODE = "EMAIL_DELIVERY_FAILED";
    private static final int STATUS = 503;

    public EmailDeliveryException(String message) {
        super(CODE, message, STATUS);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(CODE, message, STATUS, cause);
    }
}
