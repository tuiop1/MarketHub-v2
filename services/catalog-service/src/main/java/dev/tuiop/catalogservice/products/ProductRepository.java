package dev.tuiop.catalogservice.products;


import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<Product> findByIdAndMerchantId(UUID productId, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select product
            from Product product
            where product.id = :productId
              and product.merchantId = :merchantId
            """)
    Optional<Product> findLockedByIdAndMerchantId(
            @Param("productId") UUID productId,
            @Param("merchantId") UUID merchantId
    );

    Optional<Product> findByIdAndActiveTrueAndCategoryActiveTrue(UUID id);

    Page<Product> findByActiveTrueAndCategoryActiveTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category"})
    @Query("""
            select product
            from Product product
            join fetch product.category c
            where product.id in :productIds
              and product.active = true
              and product.category.active = true
            """)
    List<Product> findBuyableByIdsForUpdate(@Param("productIds") Collection<UUID> productIds);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select p
           from Product p
           join fetch p.category c
           where p.id in :ids
           order by p.id
           
           """
    )
    List<Product> findAllByIdInForUpdate(@Param("ids") Collection<UUID> ids);
}
