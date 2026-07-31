package dev.tuiop.catalogservice.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductCacheInvalidator {

    private static final String PRODUCT_CACHE = "product";

    private final CacheManager cacheManager;

    public void evict(Collection<UUID> productIds) {
        Cache productCache = cacheManager.getCache(PRODUCT_CACHE);

        if (productCache == null) {
            throw new IllegalStateException("Product cache is not configured");
        }

        productIds.forEach(productId -> productCache.evict(productId.toString()));
    }
}
