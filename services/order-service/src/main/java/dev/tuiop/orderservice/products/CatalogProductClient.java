package dev.tuiop.orderservice.products;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@HttpExchange("/api/v1/products/purchase")
public interface CatalogProductClient {

    @PostExchange("/buyable")
    List<ProductResponse> findBuyableByIdsForUpdate(@RequestBody Collection<UUID> productIds);

    @PostExchange("/stock/decrease")
    List<ProductResponse> decreaseStock(@RequestBody Collection<ProductStockDecreaseRequest> requests);
}