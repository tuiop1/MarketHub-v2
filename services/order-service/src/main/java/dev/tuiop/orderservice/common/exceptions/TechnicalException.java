package dev.tuiop.orderservice.common.exceptions;

public abstract class TechnicalException extends RuntimeException {

    private final String code;
    private final int status;

    protected TechnicalException(String code, String message, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
