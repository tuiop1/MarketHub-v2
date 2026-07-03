package dev.tuiop.accountservice.merchant.admin;

import dev.tuiop.accountservice.merchant.Merchant;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.merchant.MerchantStatus;
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


    public Merchant verifyMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.VERIFIED) {
            return merchant;
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleAdded = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleAdded = true;

            return transactionTemplate.execute(status -> {
                Merchant merchantToVerify = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                merchantToVerify.verify();
                log.info(
                        "Merchant verified by admin: merchantId={}, shopName={}",
                        merchantToVerify.getId(),
                        merchantToVerify.getShopName()
                );
                return merchantToVerify;
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

    public Merchant rejectMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.REJECTED) {
            return merchant;
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleRemoved = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleRemoved = true;

            return transactionTemplate.execute(status -> {
                Merchant merchantToReject = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                merchantToReject.reject();
                log.info(
                        "Merchant rejected by admin: merchantId={}, shopName={}",
                        merchantToReject.getId(),
                        merchantToReject.getShopName()
                );
                return merchantToReject;
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

    public Merchant suspendMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.SUSPENDED) {
            return merchant;
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleRemoved = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleRemoved = true;

            return transactionTemplate.execute(status -> {
                Merchant merchantToSuspend = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                merchantToSuspend.suspend();
                log.warn(
                        "Merchant suspended by admin: merchantId={}, shopName={}",
                        merchantToSuspend.getId(),
                        merchantToSuspend.getShopName()
                );
                return merchantToSuspend;
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

    public Merchant enableMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() == MerchantStatus.VERIFIED) {
            return merchant;
        }

        String keycloakUserId = merchant.getKeycloakUserId();
        boolean pendingRoleRemoved = false;
        boolean merchantRoleAdded = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.MERCHANT_PENDING);
            pendingRoleRemoved = true;

            keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.MERCHANT);
            merchantRoleAdded = true;

            return transactionTemplate.execute(status -> {
                Merchant merchantToEnable = merchantRepository.findById(merchantId)
                        .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

                merchantToEnable.enable();
                log.info(
                        "Merchant enabled by admin: merchantId={}, shopName={}",
                        merchantToEnable.getId(),
                        merchantToEnable.getShopName()
                );
                return merchantToEnable;
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
}
