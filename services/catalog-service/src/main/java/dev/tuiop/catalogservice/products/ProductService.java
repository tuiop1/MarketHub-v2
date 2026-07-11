package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.categories.CategoryRepository;
import dev.tuiop.catalogservice.merchants.AccountMerchantClient;
import dev.tuiop.catalogservice.merchants.MerchantResponse;
import dev.tuiop.catalogservice.merchants.MerchantStatus;
import dev.tuiop.catalogservice.merchants.exceptions.AccountServiceException;
import dev.tuiop.catalogservice.merchants.exceptions.MerchantInvalidStatusException;
import dev.tuiop.catalogservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.catalogservice.products.dto.CreateProductRequest;
import dev.tuiop.catalogservice.products.dto.ProductPurchaseResponse;
import dev.tuiop.catalogservice.products.dto.ProductResponse;
import dev.tuiop.catalogservice.products.dto.ProductStockDecreaseRequest;
import dev.tuiop.catalogservice.products.dto.ProductStockIncreaseRequest;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Product product = productRepository.findByIdAndMerchantId(productId, merchantResponse.id())
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
        validateMerchantCanManageProducts(merchantResponse);
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

    @Transactional
    public List<ProductPurchaseResponse> findBuyableByIdsForUpdate(Collection<UUID> productIds) {
        Map<UUID, Product> productsById = productRepository.findBuyableByIdsForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        validateAllProductsFound(productIds, productsById);
        Map<UUID, MerchantResponse> merchantsById = getPublicMerchantsById(productsById.values());

        return productsById.values()
                .stream()
                .map(product -> toPurchaseResponse(product, merchantsById.get(product.getMerchantId())))
                .toList();
    }

    @Transactional
    public List<ProductPurchaseResponse> decreaseStock(Collection<ProductStockDecreaseRequest> requests) {
        Map<UUID, Integer> quantitiesByProductId = mergeQuantitiesByProductId(requests);
        Map<UUID, Product> productsById = productRepository.findBuyableByIdsForUpdate(quantitiesByProductId.keySet())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        validateAllProductsFound(quantitiesByProductId.keySet(), productsById);
        Map<UUID, MerchantResponse> merchantsById = getPublicMerchantsById(productsById.values());

        for (Map.Entry<UUID, Integer> entry : quantitiesByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.decreaseStock(entry.getValue());
        }

        return productsById.values()
                .stream()
                .map(product -> toPurchaseResponse(product, merchantsById.get(product.getMerchantId())))
                .toList();
    }

    @Transactional
    public List<ProductPurchaseResponse> increaseStock(Collection<ProductStockIncreaseRequest> requests) {
        Map<UUID, Integer> quantitiesByProductId = mergeIncreaseQuantitiesByProductId(requests);
        Map<UUID, Product> productsById = productRepository.findBuyableByIdsForUpdate(quantitiesByProductId.keySet())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        validateAllProductsFound(quantitiesByProductId.keySet(), productsById);
        Map<UUID, MerchantResponse> merchantsById = getPublicMerchantsById(productsById.values());

        for (Map.Entry<UUID, Integer> entry : quantitiesByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.increaseStock(entry.getValue());
        }

        return productsById.values()
                .stream()
                .map(product -> toPurchaseResponse(product, merchantsById.get(product.getMerchantId())))
                .toList();
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
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting merchant by keycloak id", exception);
            throw AccountServiceException.unavailable(exception);
        }
    }

    private MerchantResponse getPublicMerchant(UUID merchantId, UUID productId) {
        try {
            return accountMerchantClient.getMerchant(merchantId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(Product.class, productId);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting merchant by id: merchantId={}", merchantId, exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting merchant by id: merchantId={}", merchantId, exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting merchant by id: merchantId={}", merchantId, exception);
            throw AccountServiceException.unavailable(exception);
        }
    }

    private Map<UUID, MerchantResponse> getPublicMerchantsById(Collection<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<UUID> merchantIds = products.stream()
                .map(Product::getMerchantId)
                .distinct()
                .toList();

        try {
            Map<UUID, MerchantResponse> merchantsById = accountMerchantClient.getMerchants(merchantIds)
                    .stream()
                    .collect(Collectors.toMap(MerchantResponse::id, Function.identity()));

            for (Product product : products) {
                if (!merchantsById.containsKey(product.getMerchantId())) {
                    throw new ResourceNotFoundException(Product.class, product.getId());
                }
            }

            return merchantsById;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException(Product.class, products.iterator().next().getId());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("Account service authorization failed while getting merchants by ids: merchantIds={}", merchantIds, exception);
            throw AccountServiceException.unauthorized(exception);
        } catch (ResourceAccessException exception) {
            log.warn("Account service request failed while getting merchants by ids: merchantIds={}", merchantIds, exception);
            throw AccountServiceException.unavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Account service client failed while getting merchants by ids: merchantIds={}", merchantIds, exception);
            throw AccountServiceException.unavailable(exception);
        }
    }

    private Category getCategory(UUID categoryId) {

        return categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(Category.class, categoryId));
    }

    private Map<UUID, Integer> mergeQuantitiesByProductId(Collection<ProductStockDecreaseRequest> requests) {
        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (ProductStockDecreaseRequest request : requests) {
            quantitiesByProductId.merge(request.productId(), request.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }

    private Map<UUID, Integer> mergeIncreaseQuantitiesByProductId(Collection<ProductStockIncreaseRequest> requests) {
        Map<UUID, Integer> quantitiesByProductId = new LinkedHashMap<>();

        for (ProductStockIncreaseRequest request : requests) {
            quantitiesByProductId.merge(request.productId(), request.quantity(), Integer::sum);
        }

        return quantitiesByProductId;
    }

    private void validateAllProductsFound(
            Collection<UUID> requestedProductIds,
            Map<UUID, Product> productsById
    ) {
        for (UUID requestedProductId : requestedProductIds) {
            if (!productsById.containsKey(requestedProductId)) {
                throw new ResourceNotFoundException(Product.class, requestedProductId);
            }
        }
    }

    private ProductPurchaseResponse toPurchaseResponse(Product product, MerchantResponse merchant) {


        if (merchant.status() != MerchantStatus.VERIFIED) {
            throw new ResourceNotFoundException(Product.class, product.getId());
        }

        return new ProductPurchaseResponse(
                product.getId(),
                product.getMerchantId(),
                merchant.shopName(),
                product.getCategory().getId(),
                product.getName(),
                product.getDescription(),
                product.getPriceCents(),
                product.getStockQuantity(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void validateMerchantCanManageProducts(MerchantResponse merchant) {
       if(merchant.status() != MerchantStatus.VERIFIED){
           throw new MerchantInvalidStatusException(merchant.status());
       }
    }
}
