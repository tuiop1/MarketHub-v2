package dev.tuiop.accountservice.customer.admin;

import dev.tuiop.accountservice.common.exceptions.ResourceNotFoundException;
import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
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
                .orElseThrow(() -> new ResourceNotFoundException(Customer.class, customerId));

        if (Boolean.TRUE.equals(customer.getEnabled())) {
            return;
        }

        String keycloakUserId = customer.getKeycloakUserId();
        boolean userEnabled = false;
        try {

            keycloakIdentityService.enableUser(keycloakUserId);
            userEnabled = true;

            transactionTemplate.executeWithoutResult(status -> {
                Customer customerToEnable = customerRepository.findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException(Customer.class, customerId));

                customerToEnable.enable();
            });

        } catch (RuntimeException exception) {

            if (userEnabled) {
                try {
                    keycloakIdentityService.disableUser(keycloakUserId);
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    public void disable(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.class, customerId));

        if (!Boolean.TRUE.equals(customer.getEnabled())) {
            return;
        }

        String keycloakUserId = customer.getKeycloakUserId();

        boolean userDisabled = false;

        try {
            keycloakIdentityService.disableUser(keycloakUserId);
            userDisabled = true;

            transactionTemplate.executeWithoutResult(status -> {
                Customer customerToDisable = customerRepository.findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException(Customer.class, customerId));

                customerToDisable.disable();
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
}
