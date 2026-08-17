package com.brijesh.apigateway.filter;


import com.brijesh.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    public AuthenticationFilter(JwtUtil jwtUtil, ReactiveRedisTemplate<String,String> redisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange);

            // No token provided
            if (token == null) {
                return unauthorized(exchange, "No token provided");
            }

            // Invalid token
            if (!jwtUtil.isTokenValid(token)) {
                return unauthorized(exchange, "Invalid or expired token");
            }

            // Check Redis blacklist
            String jti = jwtUtil.extractJti(token);
            String blacklistkey = BLACKLIST_PREFIX + jti;

            return redisTemplate.hasKey(blacklistkey)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.debug("Rejected blacklisted token jti={}", jti);
                            return unauthorized(exchange, "Token has been revoked");
                        }

                        // Token is valid - extract claims and add headers
                        Claims claims = jwtUtil.extractAllClaims(token);
                        String userUuid = claims.getSubject();
                        String email =  claims.get("email", String.class);
                        Object rolesObj = claims.get("roles");
                        String roles = rolesObj != null ? rolesObj.toString() : "";

                        // Add user identity headers for downstream services
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(r -> r.headers(headers -> {
                                    headers.add("X-User-Uuid", userUuid);
                                    headers.add("X-User-Email", email);
                                    headers.add("X-User-Roles", roles);
                                    // Remove original Authorization header
                                    // downstream services use headers, not JWt
                                    headers.remove(HttpHeaders.AUTHORIZATION);
                                }))
                                .build();

                        return chain.filter(mutatedExchange);
                    })
                    .onErrorResume(e -> {
                        log.error("Redis error during blacklist check: {}", e.getMessage());
                        // Fail open - Redis down shouldn't block all requests
                        Claims claims = jwtUtil.extractAllClaims(token);
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(r -> r.headers(headers -> {
                                    headers.add("X-User-Uuid", claims.getSubject());
                                    headers.add("X-User-Email", claims.get("email", String.class));
                                }))
                                .build();
                        return chain.filter(mutatedExchange);
                    });
        };
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Unauthorized: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {

    }

}
