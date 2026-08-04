package dev.tuiop.cartservice.external.products;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@HttpExchange("/internal/v1/catalog/products")
public interface InternalCatalogProductClient {

    @PostExchange("/buyable")
    List<ProductResponse> getBuyableProductsByIds(@RequestBody Collection<UUID> ids);
}
