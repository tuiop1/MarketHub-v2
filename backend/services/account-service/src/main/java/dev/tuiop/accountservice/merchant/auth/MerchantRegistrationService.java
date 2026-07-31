package dev.tuiop.accountservice.merchant.auth;

import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.kafka.AccountNotificationEventPublisher;
import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.merchant.MerchantService;
import dev.tuiop.accountservice.merchant.dto.MerchantRegistrationRequest;
import dev.tuiop.accountservice.merchant.exceptions.MerchantShopNameTakenException;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import dev.tuiop.commonevents.MerchantRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantRegistrationService {

    private final KeycloakIdentityService identityService;
    private final MerchantService merchantService;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final AccountNotificationEventPublisher eventPublisher;
    public Merchant register(
            MerchantRegistrationRequest request
    ) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String shopName = request.shopName().trim();

        if (customerRepository.existsByEmailIgnoreCase(email)
                || merchantRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        if (merchantRepository.existsByShopNameIgnoreCase(shopName)) {
            throw new MerchantShopNameTakenException(shopName);
        }

        String keycloakUserId = identityService.createUser(
                email,
                request.password(),
                request.firstName(),
                request.lastName(),
                RealmRole.MERCHANT_PENDING
        );

        try {
            Merchant merchant = merchantService.create(
                    keycloakUserId,
                    email,
                    request
            );
            publishMerchantRegisteredEvent(merchant);

            return merchant;
        } catch (RuntimeException exception) {
            try {
                identityService.deleteUser(keycloakUserId);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }
    private void publishMerchantRegisteredEvent(Merchant merchant) {
        eventPublisher.publishMerchantRegistered(
                new MerchantRegisteredEvent(
                        merchant.getId(),
                        merchant.getEmail(),
                        merchant.getShopName(),
                        Instant.now()
                )
        );
    }

}
