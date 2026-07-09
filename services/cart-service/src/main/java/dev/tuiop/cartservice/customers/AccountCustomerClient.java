package dev.tuiop.cartservice.customers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/api/v1/customers")
public interface AccountCustomerClient {

    @GetMapping("/{id}")
    CustomerResponse getCustomer(@PathVariable UUID id);

    @GetMapping("/keycloak/{keycloakUserId}")
    CustomerResponse getCustomerByKeycloakUserId(@PathVariable String keycloakUserId);
}
