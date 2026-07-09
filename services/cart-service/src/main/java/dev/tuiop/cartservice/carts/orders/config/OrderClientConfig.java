package dev.tuiop.cartservice.carts.orders.config;

import dev.tuiop.cartservice.carts.orders.OrderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class OrderClientConfig {

    @Bean
    public OrderClient orderClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.order.url}") String orderServiceUrl
    ) {
        RestClient restClient = restClientBuilder
                .baseUrl(orderServiceUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(OrderClient.class);
    }
}
