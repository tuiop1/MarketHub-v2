package dev.tuiop.catalogservice.merchants;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@HttpExchange("/api/v1/merchants")
public interface AccountMerchantClient {

    @GetMapping("/{id}")
    MerchantResponse getMerchant(@PathVariable UUID id);

    @GetMapping("/batch")
    List<MerchantResponse> getMerchants(@RequestParam Collection<UUID> merchantIds);

    @GetMapping("/keycloak/{keycloakUserId}")
    MerchantResponse getMerchantByKeycloakUserId(@PathVariable String keycloakUserId);
}
