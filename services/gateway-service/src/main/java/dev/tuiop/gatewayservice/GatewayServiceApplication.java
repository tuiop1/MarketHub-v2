package dev.tuiop.gatewayservice;

import dev.tuiop.gatewayservice.ratelimiter.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(RateLimitProperties.class)
@SpringBootApplication
public class GatewayServiceApplication {

     static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}
