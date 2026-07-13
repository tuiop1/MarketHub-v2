package dev.tuiop.accountservice.customer.auth;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.customer.CustomerService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.kafka.AccountNotificationEventPublisher;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import dev.tuiop.commonevents.CustomerRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final KeycloakIdentityService identityService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final AccountNotificationEventPublisher eventPublisher;

    public Customer register(
            CustomerRegistrationRequest request
    ) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (customerRepository.existsByEmailIgnoreCase(email)
                || merchantRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyTakenException(email);
        }

        String keycloakUserId = identityService.createUser(
                email,
                request.password(),
                request.firstName(),
                request.lastName(),
                RealmRole.CUSTOMER
        );

        try {
            Customer customer = customerService.create(
                    keycloakUserId,
                    email,
                    request
            );
            publishCustomerRegisteredEvent(customer);
            return customer;
        } catch (RuntimeException exception) {
            try {
                identityService.deleteUser(keycloakUserId);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }
    private void publishCustomerRegisteredEvent(Customer customer){
        eventPublisher.publishCustomerRegistered(
                new CustomerRegisteredEvent(
                        UUID.randomUUID(),
                        customer.getId(),
                        customer.getEmail(),
                        customer.getFirstName(),
                        Instant.now()
                )


        );
    }

}
