package dev.tuiop.gatewayservice.ratelimiter;

import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public SimpleFilterSupplier rateLimitFilterSupplier() {
        return new SimpleFilterSupplier(RateLimitFilter.class);
    }
}
