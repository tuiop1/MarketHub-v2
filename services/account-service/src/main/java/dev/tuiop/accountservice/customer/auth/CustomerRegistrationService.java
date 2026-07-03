package dev.tuiop.accountservice.customer.auth;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.CustomerService;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import dev.tuiop.accountservice.customer.mapper.CustomerMapper;
import dev.tuiop.accountservice.security.keycloak.KeycloakIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final KeycloakIdentityService identityService;
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerResponse register(
            CustomerRegistrationRequest request
    ) {
        String keycloakUserId = identityService.createUser(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                "CUSTOMER"
        );

        try {
            Customer customer = customerService.create(
                    keycloakUserId,
                    request
            );

            return customerMapper.toResponse(customer);
        } catch (RuntimeException exception) {
            identityService.deleteUser(keycloakUserId);
            throw exception;
        }
    }
}
