package dev.tuiop.cartservice.carts.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select ci
    from CartItem ci
    where ci.cart.id = :cartId
    and ci.productId = :productId
    """)
    Optional<CartItem> findByCartIdAndProductIdForUpdate(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select ci
    from CartItem ci
    where ci.cart.id = :cartId
    and ci.id = :cartItemId
    """)
    Optional<CartItem> findByCartIdAndCartItemIdForDelete(@Param("cartId") UUID cartId, @Param("cartItemId") UUID cartItemId);


}
