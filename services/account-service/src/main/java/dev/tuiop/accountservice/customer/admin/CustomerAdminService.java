package dev.tuiop.accountservice.customer.admin;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerAdminService {

    private final CustomerRepository customerRepository;
    private final KeycloakIdentityService keycloakIdentityService;
    private final TransactionTemplate transactionTemplate;

    public void enable(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer was not found with id: " + customerId
                ));

        if (Boolean.TRUE.equals(customer.getEnabled())) {
            return;
        }

        String keycloakUserId = customer.getKeycloakUserId();
        boolean roleAdded = false;

        try {
            keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.CUSTOMER);
            roleAdded = true;

            transactionTemplate.executeWithoutResult(status -> {
                Customer customerToEnable = customerRepository.findById(customerId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Customer was not found with id: " + customerId
                        ));

                customerToEnable.enable();
            });

        } catch (RuntimeException exception) {
            if (roleAdded) {
                try {
                    keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.CUSTOMER);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    public void disable(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer was not found with id: " + customerId
                ));

        if (!Boolean.TRUE.equals(customer.getEnabled())) {
            return;
        }

        String keycloakUserId = customer.getKeycloakUserId();
        boolean roleDeleted = false;

        try {
            keycloakIdentityService.removeRealmRole(keycloakUserId, RealmRole.CUSTOMER);
            roleDeleted = true;

            transactionTemplate.executeWithoutResult(status -> {
                Customer customerToDisable = customerRepository.findById(customerId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Customer was not found with id: " + customerId
                        ));

                customerToDisable.disable();
            });

        } catch (RuntimeException exception) {
            if (roleDeleted) {
                try {
                    keycloakIdentityService.addRealmRole(keycloakUserId, RealmRole.CUSTOMER);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }
}
