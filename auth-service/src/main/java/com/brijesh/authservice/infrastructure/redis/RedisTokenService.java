package com.brijesh.authservice.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    public void blacklistToken(String jti, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            return; // already expired, no need to blacklist
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.debug("Blacklisted token jti={} for {}", jti, ttl);
    }

    public boolean isTokenBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }
}