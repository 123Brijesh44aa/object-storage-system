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
        try {
            if (ttl.isNegative() || ttl.isZero()) {
                return; // already expired, no need to blacklist
            }
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "revoked", ttl);
            log.debug("Blacklisted token jti={} for {}", jti, ttl);
        } catch (Exception e){
            log.error("Redis unavailable. Could not blacklist token jti={}", jti,e);

            // Don't throw - logout should still succeed even if Redis is down
            // Token will expire naturally
        }
    }

    public boolean isTokenBlacklisted(String jti) {
        try {
            String key = BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e){
            // Redis is down - log it, but don't crash the app
            // Accept the security tradeoff: blacklisted tokens
            // might work temporarily until Redis recovers
            log.error("Redis unavailable for blacklist check. "+"Failing open. jti={}", jti,e);
            return false;   // assume not blacklisted
        }
    }
}