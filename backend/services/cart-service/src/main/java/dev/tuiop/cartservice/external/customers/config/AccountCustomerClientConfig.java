package dev.tuiop.cartservice.external.customers.config;

import dev.tuiop.cartservice.external.customers.AccountCustomerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AccountCustomerClientConfig {


    @Bean
    public AccountCustomerClient accountCustomerClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${services.account.url}") String accountMerchantUrl
    )
    {
        RestClient restClient = restClientBuilder
                .baseUrl(accountMerchantUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(AccountCustomerClient.class);
    }
}
