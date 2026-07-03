package dev.tuiop.accountservice.merchant.mapper;

import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.dto.CreateMerchantRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import org.springframework.stereotype.Component;

@Component
public class MerchantMapper {

    public Merchant toEntity(String keycloakUserId, String email, CreateMerchantRequest request) {
        return Merchant.builder()
                .keycloakUserId(keycloakUserId)
                .shopName(request.shopName().trim())
                .description(request.description())
                .email(email.trim())
                .build();
    }

    public Merchant toEntity(String keycloakUserId, MerchantRegistrationRequest request) {
        return Merchant.builder()
                .keycloakUserId(keycloakUserId)
                .shopName(request.shopName().trim())
                .description(request.description())
                .email(request.email().trim())
                .build();
    }

    public MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getShopName(),
                merchant.getDescription(),
                merchant.getEmail(),
                merchant.getStatus(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt(),
                merchant.getVerified_at()
        );
    }
}
