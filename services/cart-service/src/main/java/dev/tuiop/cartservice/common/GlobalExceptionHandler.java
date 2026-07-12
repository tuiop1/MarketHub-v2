package dev.tuiop.cartservice.common;

import dev.tuiop.cartservice.common.exceptions.BusinessException;
import dev.tuiop.cartservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.commonapi.ApiError;
import dev.tuiop.commonapi.ValidationApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND.value(),
                        exception.code(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(exception.status())
                .body(ApiError.of(
                        exception.status(),
                        exception.code(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return ResponseEntity
                .badRequest()
                .body(ValidationApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        HttpStatus.CONFLICT.value(),
                        "DATA_INTEGRITY_VIOLATION",
                        "Resource violates a unique or integrity constraint",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled runtime exception for request {}", request.getRequestURI(), exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected server error",
                        request.getRequestURI()
                ));
    }
}
