package dev.tuiop.orderservice.orders.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderConfirmationOutboxRepository extends JpaRepository<OrderConfirmationOutbox, UUID> {
    @Query("select entry.orderId from OrderConfirmationOutbox entry "
            + "where entry.publishedAt is null order by entry.createdAt, entry.orderId")
    List<UUID> findPendingOrderIds(Pageable pageable);

    // A second worker skips a row being published instead of waiting for its lock.
    @Query(value = "select * from order_confirmation_outbox "
            + "where order_id = :orderId and published_at is null for update skip locked", nativeQuery = true)
    Optional<OrderConfirmationOutbox> findPendingForUpdate(@Param("orderId") UUID orderId);
}
