package dev.tuiop.gatewayservice.ratelimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RateLimitFilterTests {

    @Test
    void disabledRateLimitingDoesNotAccessRedisOrChangeTheResponse() throws Exception {
        FixedWindowRateLimiter limiter = mock(FixedWindowRateLimiter.class);
        RateLimitFilter filter = new RateLimitFilter(limiter,
                new RateLimitProperties(false, 2, Duration.ofMinutes(1)), new ObjectMapper());
        ServerRequest request = ServerRequest.create(new MockHttpServletRequest("GET", "/api/v1/products"), List.of());
        ServerResponse expected = ServerResponse.ok().body("products");
        AtomicInteger calls = new AtomicInteger();

        ServerResponse actual = filter.filter(request, forwarded -> {
            assertThat(forwarded).isSameAs(request);
            calls.incrementAndGet();
            return expected;
        });

        assertThat(actual).isSameAs(expected);
        assertThat(calls.get()).isEqualTo(1);
        verifyNoInteractions(limiter);
    }
}
