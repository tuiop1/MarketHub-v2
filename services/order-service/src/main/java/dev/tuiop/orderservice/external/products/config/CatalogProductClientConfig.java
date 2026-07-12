package dev.tuiop.orderservice.external.products.config;

import dev.tuiop.orderservice.external.products.CatalogProductClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class CatalogProductClientConfig {

    @Bean
    public CatalogProductClient catalogProductClient(
            RestClient.Builder restClientBuilder,
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
}
