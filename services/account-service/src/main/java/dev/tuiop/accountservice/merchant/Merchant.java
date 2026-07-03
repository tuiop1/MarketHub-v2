package dev.tuiop.accountservice.merchant;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "merchants",
        indexes = {
                @Index(name = "idx_merchants_shop_name", columnList = "shop_name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String keycloakUserId;

    @Column(name = "shop_name", nullable = false, unique = true)
    private String shopName;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Instant verified_at;

    @PrePersist
    protected void prePersist() {

        createdAt = Instant.now();
        updatedAt = Instant.now();

        if (status == null) {
            status = MerchantStatus.PENDING;

        }



    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = MerchantStatus.SUSPENDED;
    }

    public void verify() {
       this.status = MerchantStatus.VERIFIED;
       this.verified_at = Instant.now();
    }

    public void reject() {
        this.status = MerchantStatus.REJECTED;
    }

    // verified is basically default, suspended is already verified and inactive
    public void enable() {
        this.status = MerchantStatus.VERIFIED;
    }

}
