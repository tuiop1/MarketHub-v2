package dev.tuiop.cartservice.external.merchants;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/api/v1/merchants")
public interface AccountMerchantClient {

    @GetExchange("/{merchantId}")
    MerchantResponse getMerchantById(@PathVariable("merchantId") UUID merchantId);

    default boolean isMerchantActive(UUID merchantId) {
        return MerchantStatus.VERIFIED.equals(getMerchantById(merchantId).status());
    }
}
