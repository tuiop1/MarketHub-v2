package dev.tuiop.gatewayservice.ratelimiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class FixedWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public FixedWindowRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String clientId, int limit, Duration windowSize) {
        long windowIndex = System.currentTimeMillis() / windowSize.toMillis();
        String key = "rate:%s:%s".formatted(clientId, windowIndex);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Redis did not return a rate-limit counter");
        }
        if (count == 1L) {
            redisTemplate.expire(key, windowSize);
        }
        return count <= limit;
    }
}
