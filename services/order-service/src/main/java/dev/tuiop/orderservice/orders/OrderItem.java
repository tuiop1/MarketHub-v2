package dev.tuiop.orderservice.orders;


import dev.tuiop.orderservice.products.ProductResponse;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_id", columnList = "product_id"),
                @Index(name = "idx_order_items_merchant_id", columnList = "merchant_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "merchant_name_snapshot", nullable = false)
    private String merchantNameSnapshot;

    @Column(name = "price_snapshot_cents", nullable = false)
    private Long priceSnapshotCents;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price_snapshot_cents", nullable = false)
    private Long totalPriceSnapshotCents;

    @PrePersist
    protected void prePersist() {
        if (totalPriceSnapshotCents == null && priceSnapshotCents != null && quantity != null) {
            totalPriceSnapshotCents = Math.multiplyExact(priceSnapshotCents, quantity);
        }
    }

    public static OrderItem fromProduct(ProductResponse product, Integer quantity){

        if(quantity <= 0) throw new IllegalArgumentException("Quantity of products must be positive ");

        return OrderItem.builder()
                .productId(product.id())
                .merchantId(product.merchantId())
                .productNameSnapshot(product.name())
                .merchantNameSnapshot(product.merchantName())
                .priceSnapshotCents(product.priceCents())
                .quantity(quantity)
                .totalPriceSnapshotCents(Math.multiplyExact(quantity, product.priceCents()))
                .build();
    }

    public void assignOrder(Order order) {
        this.order = order;
    }
}
