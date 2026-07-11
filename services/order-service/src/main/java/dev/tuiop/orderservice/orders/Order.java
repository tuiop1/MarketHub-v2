package dev.tuiop.orderservice.orders;



import dev.tuiop.orderservice.orders.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.security.core.parameters.P;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "customer_id",nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "stock_reservation_id")
    private UUID stockReservationId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_price_cents", nullable = false)
    private Long totalPriceCents;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @BatchSize(size = 50)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
   private  List<OrderItem> orderItems = new ArrayList<>();


    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = OrderStatus.PENDING_PAYMENT;
        }

        if(totalPriceCents == null){
            totalPriceCents = 0L;
        }

    }

    public void addItem(OrderItem orderItem){
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void recalculateTotalPrice(){
        long sumCents = 0L;
       for(OrderItem item : orderItems){
           sumCents+= item.getTotalPriceSnapshotCents();

       }

       totalPriceCents = sumCents;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }


    public void attachPayment(UUID paymentId){
        if(this.paymentId != null) {
            return;

        }

        this.paymentId = paymentId;
    }

    public void markPaid() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order is not pending payment");
        }

        status = OrderStatus.PAID;
    }

    public void markPaymentFailed(String reason) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("Paid order cannot become failed");
        }

        status = OrderStatus.PAYMENT_FAILED;
        failureReason = reason;
    }

    public void cancel(String reason) {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("Paid order cannot be cancelled without refund");
        }

        status = OrderStatus.CANCELLED;
        failureReason = reason;
    }

    public void markCompensationFailed(String reason) {
        status = OrderStatus.COMPENSATION_FAILED;
        failureReason = reason;
    }
}
