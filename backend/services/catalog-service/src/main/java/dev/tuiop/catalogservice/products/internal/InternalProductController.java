package dev.tuiop.catalogservice.products.internal;

import dev.tuiop.catalogservice.products.ProductService;
import dev.tuiop.catalogservice.products.dto.ProductPurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/catalog/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @PostMapping("/buyable")
    public ResponseEntity<List<ProductPurchaseResponse>> findBuyableProductsForPurchase(
            @RequestBody Collection<UUID> productIds
    ) {
        return ResponseEntity.ok(productService.findBuyableByIdsForUpdate(productIds));
    }
}
