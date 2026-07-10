package dev.tuiop.cartservice.products;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


@HttpExchange("/api/v1/products")
public interface CatalogProductClient {

    @GetExchange("/{productId}")
    ProductResponse getProductById(@PathVariable UUID productId);

    @PostExchange("/purchase/buyable")
    List<ProductResponse> getBuyableProductsByIds(@RequestBody Collection<UUID> ids);

}