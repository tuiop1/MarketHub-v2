package dev.tuiop.cartservice.categories;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/api/v1/categories")
public interface CatalogCategoryClient {

    @GetExchange("/{categoryId}")
    CategoryResponse getCategoryById(@PathVariable("categoryId") UUID categoryId);

    default boolean isCategoryActive(UUID categoryId) {
        return Boolean.TRUE.equals(getCategoryById(categoryId).active());
    }
}
