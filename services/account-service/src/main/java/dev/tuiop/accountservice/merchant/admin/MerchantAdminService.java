package dev.tuiop.accountservice.merchant.admin;

import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.merchant.MerchantStatus;
import dev.tuiop.accountservice.merchant.exceptions.MerchantInvalidStatusTransitionException;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class MerchantAdminService {
    private final MerchantRepository merchantRepository;
    private final KeycloakIdentityService keycloakIdentityService;
    private final TransactionTemplate transactionTemplate;


    @Transactional(readOnly = true)
    public Page<Merchant> getUnverifiedMerchants(Pageable pageable) {
        return merchantRepository.findByStatus(MerchantStatus.PENDING, pageable);
    }


    public void verifyMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.VERIFIED) {
            return;
        }
        if(merchant.getStatus() != MerchantStatus.PENDING){
            throw new MerchantInvalidStatusTransitionException(merchant.getStatus(), MerchantStatus.VERIFIED);
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleAdded = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleAdded = true;

            transactionTemplate.executeWithoutResult(status -> {
                Merchant merchantToVerify = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                verify(merchantToVerify);
                log.info(
                        "Merchant verified by admin: merchantId={}, shopName={}",
                        merchantToVerify.getId(),
                        merchantToVerify.getShopName()
                );
            });
        } catch (RuntimeException exception) {
            if (merchantRoleAdded) {
                try {
                    keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            if (pendingRoleRemoved && merchant.getStatus() == MerchantStatus.PENDING) {
                try {
                    keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    public void rejectMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.REJECTED) {
            return;
        }
        if(merchant.getStatus() != MerchantStatus.PENDING){
            throw new MerchantInvalidStatusTransitionException(merchant.getStatus(), MerchantStatus.REJECTED);
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleRemoved = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleRemoved = true;

            transactionTemplate.executeWithoutResult(status -> {
                Merchant merchantToReject = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                reject(merchantToReject);
                log.info(
                        "Merchant rejected by admin: merchantId={}, shopName={}",
                        merchantToReject.getId(),
                        merchantToReject.getShopName()
                );
            });
        } catch (RuntimeException exception) {
            if (merchantRoleRemoved && merchant.getStatus() == MerchantStatus.VERIFIED) {
                try {
                    keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            if (pendingRoleRemoved && merchant.getStatus() == MerchantStatus.PENDING) {
                try {
                    keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    public void suspendMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.SUSPENDED) {
            return;
        }
        if (merchant.getStatus() != MerchantStatus.VERIFIED) {
            throw new MerchantInvalidStatusTransitionException(merchant.getStatus(), MerchantStatus.SUSPENDED);
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean userDisabled = false;

        try {
            keycloakIdentityService.disableUser(keycloakUserId);
            userDisabled = true;

            transactionTemplate.executeWithoutResult(status -> {
                Merchant merchantToSuspend = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                suspend(merchantToSuspend);
                log.warn(
                        "Merchant suspended by admin: merchantId={}, shopName={}",
                        merchantToSuspend.getId(),
                        merchantToSuspend.getShopName()
                );
            });
        } catch (RuntimeException exception) {
            if (userDisabled) {
                try {
                    keycloakIdentityService.enableUser(keycloakUserId);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    private void verify(Merchant merchant){
        MerchantStatus status = merchant.getStatus();
        if(status == MerchantStatus.VERIFIED){
            return;
        }
        if(status != MerchantStatus.PENDING){
            throw new MerchantInvalidStatusTransitionException(status, MerchantStatus.VERIFIED  );
        }

        merchant.verify();
    }

    private void reject(Merchant merchant) {
        MerchantStatus status = merchant.getStatus();
        if (status == MerchantStatus.REJECTED) {
            return;
        }
        if (status != MerchantStatus.PENDING) {
            throw new MerchantInvalidStatusTransitionException(status, MerchantStatus.REJECTED);
        }

        merchant.reject();
    }

    private void suspend(Merchant merchant) {
        MerchantStatus status = merchant.getStatus();
        if (status == MerchantStatus.SUSPENDED) {
            return;
        }
        if (status != MerchantStatus.VERIFIED) {
            throw new MerchantInvalidStatusTransitionException(status, MerchantStatus.SUSPENDED);
        }

        merchant.suspend();
    }

}
