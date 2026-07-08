package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.products.dto.CreateProductRequest;
import dev.tuiop.catalogservice.products.dto.ProductResponse;
import dev.tuiop.catalogservice.products.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createMyProduct(jwt, request));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        return ResponseEntity.ok( productService.getMyProducts(jwt, pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getMyProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok( productService.getMyProduct(jwt, productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateMyProduct(jwt, productId, request));
    }

    @DeleteMapping("/{productId}")
    public void deleteProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        productService.deleteMyProduct(jwt, productId);
    }
}
