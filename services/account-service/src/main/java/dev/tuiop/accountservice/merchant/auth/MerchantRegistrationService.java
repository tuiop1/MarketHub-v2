package dev.tuiop.accountservice.merchant.auth;

import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.merchant.MerchantService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.exceptions.ShopNameAlreadyTakenException;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantRegistrationService {

    private final KeycloakIdentityService identityService;
    private final MerchantService merchantService;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    public Merchant register(
            MerchantRegistrationRequest request
    ) {
        String email = request.email().trim();
        String shopName = request.shopName().trim();

        if (customerRepository.existsByEmailIgnoreCase(email)
                || merchantRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        if (merchantRepository.existsByShopNameIgnoreCase(shopName)) {
            throw new ShopNameAlreadyTakenException(shopName);
        }

        String keycloakUserId = identityService.createUser(
                email,
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

            return merchant;
        } catch (RuntimeException exception) {
            identityService.deleteUser(keycloakUserId);
            throw exception;
        }
    }
}
