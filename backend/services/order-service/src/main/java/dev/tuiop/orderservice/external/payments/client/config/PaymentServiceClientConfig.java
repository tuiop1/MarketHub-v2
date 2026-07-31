package dev.tuiop.orderservice.external.payments.client.config;

import dev.tuiop.orderservice.external.payments.client.PaymentServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class PaymentServiceClientConfig {

    @Bean
    public PaymentServiceClient paymentServiceClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${services.payment.url}") String paymentServiceUrl
    ) {
        RestClient restClient = restClientBuilder
                .baseUrl(paymentServiceUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(PaymentServiceClient.class);
    }
}
