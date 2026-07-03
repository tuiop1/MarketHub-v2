package dev.tuiop.accountservice.merchant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    boolean existsByShopNameIgnoreCase(String shopName);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByKeycloakUserId(String keycloakUserId);

    Optional<Merchant> findByKeycloakUserId(String keycloakUserId);
}
