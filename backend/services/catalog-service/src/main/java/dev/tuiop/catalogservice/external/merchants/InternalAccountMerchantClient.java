package dev.tuiop.catalogservice.external.merchants;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@HttpExchange("/internal/v1/accounts/merchants")
public interface InternalAccountMerchantClient {

    @GetExchange("/batch")
    List<MerchantResponse> getMerchants(@RequestParam("merchantIds") Collection<UUID> merchantIds);

    @GetExchange("/keycloak/{keycloakUserId}")
    MerchantResponse getMerchantByKeycloakUserId(@PathVariable("keycloakUserId") String keycloakUserId);
}
