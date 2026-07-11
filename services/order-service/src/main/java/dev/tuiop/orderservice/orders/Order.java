package dev.tuiop.orderservice.orders;



import dev.tuiop.orderservice.orders.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

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


}
