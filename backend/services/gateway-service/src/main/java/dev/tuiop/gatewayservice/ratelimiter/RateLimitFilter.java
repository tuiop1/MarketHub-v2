package dev.tuiop.gatewayservice.ratelimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tuiop.commonapi.ApiError;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            FixedWindowRateLimiter fixedWindowRateLimiter,
            RateLimitProperties rateLimitProperties,
            ObjectMapper objectMapper
    ) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitProperties.enabled()) {
            return chain.filter(exchange);
        }

        String client = clientIp(exchange);

        return fixedWindowRateLimiter
                .allowRequest(client, rateLimitProperties.requestLimit(), rateLimitProperties.windowSize())
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange));
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwardedIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (forwardedIp != null && !forwardedIp.isBlank()) {
            return forwardedIp.trim();
        }

        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(address -> address.getHostAddress())
                .orElse("unknown");
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        ApiError apiError = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many requests. Please try again later.",
                exchange.getRequest().getPath().value()
        );

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(
                "Retry-After",
                String.valueOf(rateLimitProperties.windowSize().toSeconds())
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(apiError);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
