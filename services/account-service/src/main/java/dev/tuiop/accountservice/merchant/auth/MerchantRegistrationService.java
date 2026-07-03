package dev.tuiop.accountservice.merchant.auth;

import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.MerchantService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.dto.MerchantResponse;
import dev.tuiop.accountservice.merchant.mapper.MerchantMapper;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantRegistrationService {

    private final KeycloakIdentityService identityService;
    private final MerchantService merchantService;
    private final MerchantMapper merchantMapper;

    public MerchantResponse register(
            MerchantRegistrationRequest request
    ) {
        String keycloakUserId = identityService.createUser(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                "MERCHANT_PENDING"
        );

        try {
            Merchant merchant = merchantService.create(
                    keycloakUserId,
                    request
            );

            return merchantMapper.toResponse(merchant);
        } catch (RuntimeException exception) {
            identityService.deleteUser(keycloakUserId);
            throw exception;
        }
    }
}
