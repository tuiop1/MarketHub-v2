package dev.tuiop.accountservice.customer.auth;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerRepository;
import dev.tuiop.accountservice.customer.CustomerService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.common.exceptions.EmailAlreadyTakenException;
import dev.tuiop.accountservice.merchant.MerchantRepository;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import dev.tuiop.accountservice.security.keycloak.RealmRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final KeycloakIdentityService identityService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    public Customer register(
            CustomerRegistrationRequest request
    ) {
        String email = request.email().trim().toLowerCase();

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
                    request
            );

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


}
