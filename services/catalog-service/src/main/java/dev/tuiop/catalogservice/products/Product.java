package dev.tuiop.catalogservice.products;



import dev.tuiop.catalogservice.categories.Category;
import dev.tuiop.catalogservice.products.exceptions.InsufficientStockException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_name", columnList = "name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false)
    private Long priceCents;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }

        if (stockQuantity == null) {
            stockQuantity = 0;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }



    public void update(
            String name,
            String description,
            Long priceCents,
            Integer stockQuantity,
            Category category,
            Boolean active
    ) {
        if (priceCents == null || priceCents <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (stockQuantity < quantity) {
            throw new InsufficientStockException(name, quantity, stockQuantity);
        }

        stockQuantity -= quantity;
    }

}
