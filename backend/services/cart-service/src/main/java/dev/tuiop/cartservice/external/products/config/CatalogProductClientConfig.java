package dev.tuiop.cartservice.external.products.config;

import dev.tuiop.cartservice.external.products.CatalogProductClient;
import dev.tuiop.cartservice.external.products.InternalCatalogProductClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class CatalogProductClientConfig {

    @Bean
    public CatalogProductClient catalogProductClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${services.catalog.url}") String catalogServiceUrl
    ) {
        RestClient restClient = restClientBuilder
                .baseUrl(catalogServiceUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(CatalogProductClient.class);
    }

    @Bean
    public InternalCatalogProductClient internalCatalogProductClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${services.catalog.url}") String catalogServiceUrl
    ) {
        RestClient restClient = restClientBuilder
                .baseUrl(catalogServiceUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(InternalCatalogProductClient.class);
    }
}
