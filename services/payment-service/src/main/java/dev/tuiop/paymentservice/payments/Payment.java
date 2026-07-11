package dev.tuiop.paymentservice.payments;

import dev.tuiop.paymentservice.payments.enums.PaymentMethod;
import dev.tuiop.paymentservice.payments.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_payments_order_id",
                columnNames = "order_id"
        )
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Payment {



    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "order_id", nullable = false, updatable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private Long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod method;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;




    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = PaymentStatus.PENDING;
        }

        if(amountCents == null){
            amountCents = 0L;
        }



    }
    @PreUpdate
    protected void preUpdate(){
        updatedAt = Instant.now();
    }

    public void markSucceeded() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payment can succeed");
        }

        status = PaymentStatus.SUCCEEDED;
    }

    public void markFailed(String reason) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payment can fail");
        }

        status = PaymentStatus.FAILED;
        failureReason = reason;
    }

    public void cancelOrRefund(String reason) {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.CANCELLED;
            failureReason = reason;
            return;
        }

        if (status == PaymentStatus.SUCCEEDED) {
            status = PaymentStatus.REFUNDED;
            failureReason = reason;
            
        }

    }
}
