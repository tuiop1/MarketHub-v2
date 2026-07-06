package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.categories.CategoryRepository;
import dev.tuiop.catalogservice.client.merchants.AccountMerchantClient;
import dev.tuiop.catalogservice.client.merchants.MerchantResponse;
import dev.tuiop.catalogservice.client.merchants.MerchantStatus;
import dev.tuiop.catalogservice.client.merchants.exceptions.MerchantInvalidStatusException;
import dev.tuiop.catalogservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.catalogservice.products.dto.CreateProductRequest;
import dev.tuiop.catalogservice.products.dto.ProductResponse;
import dev.tuiop.catalogservice.products.dto.UpdateProductRequest;
import dev.tuiop.catalogservice.products.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final AccountMerchantClient accountMerchantClient;


    @PreAuthorize("hasRole('MERCHANT')")
    @Transactional
    public ProductResponse createMyProduct(Jwt jwt, CreateProductRequest request) {

        MerchantResponse merchantResponse = getMerchantByKeycloakUserId(jwt.getSubject());

        validateMerchantCanManageProducts(merchantResponse);

        Category category = getCategory(request.categoryId());

        Product product = Product.builder()
                .merchantId(merchantResponse.id())
                .category(category)
                .name(request.name().trim())
                .description(request.description())
                .priceCents(request.priceCents())
                .stockQuantity(request.stockQuantity())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info(
                "Product created: productId={}, merchantId={}, categoryId={}, name={}, priceCents={}, stockQuantity={}",
                savedProduct.getId(),
                merchantResponse.id(),
                category.getId(),
                savedProduct.getName(),
                savedProduct.getPriceCents(),
                savedProduct.getStockQuantity()
        );
        return productMapper.toResponse(savedProduct);
    }

    @PreAuthorize("hasRole('MERCHANT')")
    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(Jwt jwt, Pageable pageable) {
        MerchantResponse merchantResponse = getMerchantByKeycloakUserId(jwt.getSubject());

        return productRepository.findByMerchantId(merchantResponse.id(), pageable).map(productMapper::toResponse);
    }

    @PreAuthorize("hasRole('MERCHANT')")
    @Transactional(readOnly = true)
    public ProductResponse getMyProduct(Jwt jwt, UUID productId) {
        MerchantResponse merchantResponse = getMerchantByKeycloakUserId(jwt.getSubject());

        Product product = productRepository.findLockedByIdAndMerchantId(productId, merchantResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException(Product.class, productId));

        return productMapper.toResponse(product);
    }

    @PreAuthorize("hasRole('MERCHANT')")
    @Transactional
    public ProductResponse updateMyProduct(
            Jwt jwt,
            UUID productId,
            UpdateProductRequest request
    ) {
        MerchantResponse merchantResponse = getMerchantByKeycloakUserId(jwt.getSubject());

        validateMerchantCanManageProducts(merchantResponse);

        Product product = productRepository.findLockedByIdAndMerchantId(productId, merchantResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException(Product.class, productId));

        Category category = getCategory(request.categoryId());

        product.update(
                request.name().trim(),
                request.description(),
                request.priceCents(),
                request.stockQuantity(),
                category,
                request.active()
        );

        log.info(
                "Product updated: productId={}, merchantId={}, categoryId={}, name={}, priceCents={}, stockQuantity={}, active={}",
                product.getId(),
                merchantResponse.id(),
                category.getId(),
                product.getName(),
                product.getPriceCents(),
                product.getStockQuantity(),
                product.getActive()
        );
        return productMapper.toResponse(product);
    }

    @PreAuthorize("hasRole('MERCHANT')")
    @Transactional
    public void deleteMyProduct(Jwt jwt, UUID productId) {
        MerchantResponse merchantResponse = getMerchantByKeycloakUserId(jwt.getSubject());

        Product product = productRepository.findByIdAndMerchantId(productId, merchantResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException(Product.class, productId));

        product.deactivate();
        log.warn(
                "Product deactivated: productId={}, merchantId={}, name={}",
                product.getId(),
                merchantResponse.id(),
                product.getName()
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(Pageable pageable) {
        return productRepository.findByActiveTrueAndCategoryActiveTrue(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(UUID productId) {
        Product product = productRepository.findByIdAndActiveTrueAndCategoryActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException(Product.class, productId));

        MerchantResponse merchantResponse = getPublicMerchant(product.getMerchantId(), productId);

        if(merchantResponse.status() != MerchantStatus.VERIFIED){
            throw new ResourceNotFoundException(Product.class, productId);
        }


        return productMapper.toResponse(product);
    }

//    private Page<ProductResponse> toResponsePageWithImages(Page<Product> products) {
//        List<UUID> productIds = products.getContent()
//                .stream()
//                .map(Product::getId)
//                .toList();
//
//        if (productIds.isEmpty()) {
//            return products.map(product -> productMapper.toResponse(product, List.of()));
//        }
//
//        Map<UUID, List<ProductImageResponse>> imagesByProductId = productImageRepository.findByProductIdIn(productIds)
//                .stream()
//                .collect(Collectors.groupingBy(
//                        image -> image.getProduct().getId(),
//                        Collectors.mapping(productImageMapper::toProductImageResponse, Collectors.toList())
//                ));
//
//        return products.map(product -> productMapper.toResponse(
//                product,
//                imagesByProductId.getOrDefault(product.getId(), List.of())
//        ));
//    }

    private MerchantResponse getMerchantByKeycloakUserId(String keycloakId) {
        try {
            return accountMerchantClient.getMerchantByKeycloakUserId(keycloakId);
        } catch (HttpClientErrorException.NotFound exception ){
            throw new ResourceNotFoundException(MerchantResponse.class, "keycloakId", keycloakId);
        }
    }

    private MerchantResponse getPublicMerchant(UUID merchantId, UUID productId) {
        try {
            return accountMerchantClient.getMerchant(merchantId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(Product.class, productId);
        }
    }

    private Category getCategory(UUID categoryId) {

        return categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
    }

    private void validateMerchantCanManageProducts(MerchantResponse merchant) {
       if(merchant.status() != MerchantStatus.VERIFIED){
           throw new MerchantInvalidStatusException(merchant.status());
       }
    }
}
