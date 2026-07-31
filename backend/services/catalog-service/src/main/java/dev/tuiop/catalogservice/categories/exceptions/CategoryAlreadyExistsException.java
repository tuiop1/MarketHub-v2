package dev.tuiop.catalogservice.categories.exceptions;

import dev.tuiop.catalogservice.common.exceptions.BusinessException;

public class CategoryAlreadyExistsException extends BusinessException {
    public CategoryAlreadyExistsException(String name) {
        super("CATEGORY_ALREADY_EXISTS", "Category with the name: " + name + " already exists", 409);
    }
}
