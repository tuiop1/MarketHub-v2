package dev.tuiop.catalogservice.external.merchants.config;

import dev.tuiop.catalogservice.external.merchants.AccountMerchantClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AccountMerchantClientConfig {


    @Bean
    public AccountMerchantClient accountMerchantClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.account.url}") String accountMerchantUrl
    )
    {
        RestClient restClient = restClientBuilder
                .baseUrl(accountMerchantUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(AccountMerchantClient.class);
    }
}
