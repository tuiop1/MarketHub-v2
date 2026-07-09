package dev.tuiop.cartservice.products;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;


@HttpExchange("/api/v1/products")
public interface CatalogProductClient {

    @GetExchange("/{productId}")
    ProductResponse getProductById(@PathVariable UUID productId);
}