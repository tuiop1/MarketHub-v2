package dev.tuiop.orderservice.common.exceptions;

public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final int status;

    protected BusinessException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected BusinessException(String code, String message, int status, Throwable cause) {
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
