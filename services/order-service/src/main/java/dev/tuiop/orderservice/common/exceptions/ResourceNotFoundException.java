package dev.tuiop.orderservice.common.exceptions;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(Class<?> resourceType, Object id) {
        super(
                "RESOURCE_NOT_FOUND",
                resourceType.getSimpleName() + " was not found with id: " + id,
                404
        );
    }

    public ResourceNotFoundException(Class<?> resourceType, String field, Object value) {
        super(
                "RESOURCE_NOT_FOUND",
                resourceType.getSimpleName() + " was not found with " + field + ": " + value,
                404
        );
    }
}
