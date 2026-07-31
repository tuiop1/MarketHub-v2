package dev.tuiop.accountservice.common.exceptions;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(Class<?> resourceType, Object id) {
        this(resourceType, "id", id);
    }

    public ResourceNotFoundException(Class<?> resourceType, String field, Object value) {
        super(
                "RESOURCE_NOT_FOUND",
                resourceType.getSimpleName() + " was not found with " + field + ": " + value,
                404
        );
    }
}
