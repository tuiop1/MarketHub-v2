package dev.tuiop.catalogservice.categories.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

public class InactiveCategoryException extends BusinessException {
    public InactiveCategoryException(String name) {
        super("INACTIVE_CATEGORY", "Category " + name + " is inactive", 409);
    }
}
