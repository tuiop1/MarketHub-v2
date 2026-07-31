package dev.tuiop.gatewayservice.ratelimiter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class FixedWindowRateLimiter {

    private final ReactiveStringRedisTemplate redisTemplate;

    public FixedWindowRateLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> allowRequest(String clientId, int limit, Duration windowSize) {
        long windowIndex = System.currentTimeMillis() / windowSize.toMillis();
        String key = "rate:%s:%s".formatted(clientId, windowIndex);

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(key, windowSize)
                                .thenReturn(count <= limit);
                    }
                    return Mono.just(count <= limit);
                });
    }
}
