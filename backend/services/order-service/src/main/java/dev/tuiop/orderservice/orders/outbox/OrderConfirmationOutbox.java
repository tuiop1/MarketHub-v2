package dev.tuiop.orderservice.orders.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_confirmation_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderConfirmationOutbox {
    // One confirmation per order; also the stable Kafka message key.
    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public OrderConfirmationOutbox(UUID orderId, String payload, Instant createdAt) {
        this.orderId = orderId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished() {
        publishedAt = Instant.now();
    }
}
