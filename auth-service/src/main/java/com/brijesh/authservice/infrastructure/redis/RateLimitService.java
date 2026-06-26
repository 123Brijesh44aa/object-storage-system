package com.brijesh.authservice.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String,Object> redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    // Returns true if the request is allowed, false if rate limit exceeded.
    public boolean isAllowed(String key, int maxRequests, Duration window){
        String redisKey = RATE_LIMIT_PREFIX + key;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1){
            // Fire request in this window - set expiry
            redisTemplate.expire(redisKey,window);
        }

        return currentCount != null && currentCount <= maxRequests;
    }
}
