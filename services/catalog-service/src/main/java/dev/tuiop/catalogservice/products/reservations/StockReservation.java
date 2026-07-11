package dev.tuiop.catalogservice.products.reservations;

import dev.tuiop.catalogservice.products.Product;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Builder
@Getter
public class StockReservation {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockReservationItem> items = new ArrayList<>();


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public void release(Map<UUID, Product> productsById) {
        if (status == StockReservationStatus.RELEASED) {
            return;
        }

        if (status == StockReservationStatus.COMMITTED) {
            throw new IllegalStateException("Committed reservation cannot be released");
        }

        for (StockReservationItem item : items) {
            Product product = productsById.get(item.getProductId());
            product.releaseStock(item.getQuantity());
        }

        status = StockReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public void commit() {
        if (status == StockReservationStatus.COMMITTED) {
            return;
        }

        if (status == StockReservationStatus.RELEASED) {
            throw new IllegalStateException("Released reservation cannot be committed");
        }

        status = StockReservationStatus.COMMITTED;
        updatedAt = Instant.now();
    }
    public void addItem(Product product, Integer quantity){
        StockReservationItem stockReservationItem = StockReservationItem.fromProduct(product, this, quantity );
        this.items.add(stockReservationItem);
        updatedAt = Instant.now();

    }

    @PrePersist
    public void prePersist(){
        if(status == null ){
            this.status = StockReservationStatus.RESERVED;
        }
        if(createdAt == null){
            this.createdAt = Instant.now();
        }
        if(updatedAt == null){
            this.updatedAt = Instant.now();
        }
    }
}
