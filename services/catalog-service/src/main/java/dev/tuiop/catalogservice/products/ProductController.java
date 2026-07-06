package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.products.dto.ProductResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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



//    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ProductImageResponse> uploadImage(
//            @AuthenticationPrincipal Jwt principal,
//            @PathVariable UUID productId,
//            @RequestPart("image") MultipartFile file,
//            @RequestParam("position") @Min(1) Integer position
//    ) throws IOException {
//        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.upload( file, productId, position, principal));
//
//    }



//    @PreAuthorize("hasRole('MERCHANT')")
//    @DeleteMapping(value = "/{productId}/images/{imageId}")
//    public ResponseEntity<Void> deleteImage(
//            @AuthenticationPrincipal Jwt principal,
//            @PathVariable UUID productId,
//            @PathVariable UUID imageId
//    )  {
//
//
//
//        productImageService.delete(imageId, productId, principal);
//
//        return ResponseEntity.ok().build();
//
//    }
}
