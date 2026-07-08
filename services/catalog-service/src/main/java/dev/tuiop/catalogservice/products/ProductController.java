package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.products.dto.ProductResponse;
import dev.tuiop.catalogservice.products.dto.ProductPurchaseResponse;
import dev.tuiop.catalogservice.products.dto.ProductStockDecreaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse> > getProducts(Pageable pageable) {
        return ResponseEntity.ok( productService.getPublicProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity< ProductResponse> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok( productService.getPublicProduct(productId));
    }

    @PostMapping("/purchase/buyable")
    public ResponseEntity<List<ProductPurchaseResponse>> findBuyableProductsForPurchase(
            @RequestBody Collection<UUID> productIds
    ) {
        return ResponseEntity.ok(productService.findBuyableByIdsForUpdate(productIds));
    }

    @PostMapping("/purchase/stock/decrease")
    public ResponseEntity<List<ProductPurchaseResponse>> decreaseStockForPurchase(
            @Valid @RequestBody Collection<ProductStockDecreaseRequest> requests
    ) {
        return ResponseEntity.ok(productService.decreaseStock(requests));
    }



//    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ProductImageResponse> uploadImage(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable UUID productId,
//            @RequestPart("image") MultipartFile file,
//            @RequestParam("position") @Min(1) Integer position
//    ) throws IOException {
//        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.upload( file, productId, position, jwt));
//
//    }



//    @PreAuthorize("hasRole('MERCHANT')")
//    @DeleteMapping(value = "/{productId}/images/{imageId}")
//    public ResponseEntity<Void> deleteImage(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable UUID productId,
//            @PathVariable UUID imageId
//    )  {
//
//
//
//        productImageService.delete(imageId, productId, jwt);
//
//        return ResponseEntity.ok().build();
//
//    }
}
