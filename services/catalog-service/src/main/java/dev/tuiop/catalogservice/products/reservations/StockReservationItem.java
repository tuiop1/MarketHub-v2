package dev.tuiop.catalogservice.products.reservations;

import dev.tuiop.catalogservice.products.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "stock_reservation_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class StockReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id",nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Column(name = "reservation_id", nullable = false)
    private StockReservation reservation;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private Long unitPriceCents;

    @Column(nullable = false, updatable = false)
    private Integer quantity;


    public Long getTotalPriceCents(){
        return unitPriceCents * quantity;
    }
    public static StockReservationItem fromProduct(Product product, StockReservation stockReservation, Integer quantity
   ){
        return StockReservationItem.builder()
                .reservation(stockReservation)
                .unitPriceCents(product.getPriceCents())
                .productName(product.getName())
                .quantity(quantity)
                .productId(product.getId())
                .merchantId(product.getMerchantId())


                .build();


    }
}
