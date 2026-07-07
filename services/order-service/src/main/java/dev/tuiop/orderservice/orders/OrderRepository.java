package dev.tuiop.orderservice.orders;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "orderItems.merchant"
    })
    Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}