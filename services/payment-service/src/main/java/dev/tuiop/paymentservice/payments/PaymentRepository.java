package dev.tuiop.paymentservice.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Boolean existsByOrderId(UUID orderId);

    Optional<Payment> findByOrderId(UUID orderId);
}

