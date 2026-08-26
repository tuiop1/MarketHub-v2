package dev.tuiop.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
        "app.rate-limit.enabled=false"
})
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
