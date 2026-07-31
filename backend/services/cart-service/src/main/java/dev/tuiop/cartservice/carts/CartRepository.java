package dev.tuiop.cartservice.carts;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cartItems"})
    @Query("""
            select c
            from Cart c
            where c.customerId = :customerId
            """)
    Optional<Cart> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);

   @EntityGraph(attributePaths = {"cartItems"})
    Optional<Cart> findCartByCustomerId( UUID customerId);

    boolean existsByCustomerId(UUID id);

    Optional<Cart> findByCustomerId(UUID customerId);
}
