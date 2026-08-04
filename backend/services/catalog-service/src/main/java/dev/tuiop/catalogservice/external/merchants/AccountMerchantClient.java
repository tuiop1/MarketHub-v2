package dev.tuiop.catalogservice.external.merchants;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/api/v1/merchants")
public interface AccountMerchantClient {

    @GetExchange("/{id}")
    MerchantResponse getMerchant(@PathVariable("id") UUID id);
}
