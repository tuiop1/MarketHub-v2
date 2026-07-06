package dev.tuiop.catalogservice.client.merchants;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/api/v1/merchants")
public interface AccountMerchantClient {

    @GetMapping("/{id}")
    MerchantResponse getMerchant(@PathVariable UUID id);

    @GetMapping("/keycloak/{keycloakUserId}")
    MerchantResponse getMerchantByKeycloakUserId(@PathVariable String keycloakUserId);
}
