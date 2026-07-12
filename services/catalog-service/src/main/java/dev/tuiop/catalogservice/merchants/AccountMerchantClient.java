package dev.tuiop.catalogservice.merchants;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@HttpExchange("/api/v1/merchants")
public interface AccountMerchantClient {

    @GetExchange("/{id}")
    MerchantResponse getMerchant(@PathVariable("id") UUID id);

    @GetExchange("/batch")
    List<MerchantResponse> getMerchants(@RequestParam("merchantIds") Collection<UUID> merchantIds);

    @GetExchange("/keycloak/{keycloakUserId}")
    MerchantResponse getMerchantByKeycloakUserId(@PathVariable("keycloakUserId") String keycloakUserId);
}
