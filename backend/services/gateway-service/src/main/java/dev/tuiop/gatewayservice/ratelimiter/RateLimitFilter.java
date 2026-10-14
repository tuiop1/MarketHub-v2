package dev.tuiop.gatewayservice.ratelimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tuiop.commonapi.ApiError;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;

@Component
public class RateLimitFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

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

    // Spring Cloud Gateway discovers this factory for the YAML route filter.
    public static HandlerFilterFunction<ServerResponse, ServerResponse> fixedWindowRateLimit() {
        return (request, next) -> MvcUtils.getApplicationContext(request)
                .getBean(RateLimitFilter.class)
                .filter(request, next);
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        if (!rateLimitProperties.enabled()) {
            return next.handle(request);
        }

        boolean allowed = fixedWindowRateLimiter.allowRequest(
                clientIp(request), rateLimitProperties.requestLimit(), rateLimitProperties.windowSize());
        return allowed ? next.handle(request) : reject(request);
    }

    private String clientIp(ServerRequest request) {
        String forwardedIp = request.headers().firstHeader("X-Real-IP");
        if (forwardedIp != null && !forwardedIp.isBlank()) {
            return forwardedIp.trim();
        }

        String remoteAddress = request.servletRequest().getRemoteAddr();
        return remoteAddress != null ? remoteAddress : "unknown";
    }

    private ServerResponse reject(ServerRequest request) throws Exception {
        ApiError apiError = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many requests. Please try again later.",
                request.path()
        );

        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimitProperties.windowSize().toSeconds()))
                .body(objectMapper.writeValueAsBytes(apiError));
    }
}
