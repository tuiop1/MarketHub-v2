package dev.tuiop.gatewayservice.ratelimiter;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
public record RateLimitProperties(
        boolean enabled,
        @Min(1)
        int requestLimit,
        @NotNull
        Duration windowSize
) {
}
