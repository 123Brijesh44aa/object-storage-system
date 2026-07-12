package com.brijesh.authservice.security;


import com.brijesh.authservice.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    // Token Generation

    // Generate Access Token
    public String generateAccessToken(CustomUserDetails userDetails){
        return buildToken(
                Map.of(
                        "email",userDetails.getEmail(),
                        "roles",userDetails.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .toList()
                ),
                userDetails.getUuid(),
                appProperties.getJwt().getAccessTokenExpiry()
        );
    }

    // Generate Refresh Token
    public String generateRefreshToken(CustomUserDetails userDetails){
        return buildToken(
                Map.of(), // Refresh token carries minimal data
                userDetails.getUuid(),
                appProperties.getJwt().getRefreshTokenExpiry()
        );
    }


    private String buildToken(Map<String, Object> extraClaims,String subject,long expiry){
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())  // jti - unique token ID
                .subject(subject)                  // uuid - never expose DB id
                .claims(extraClaims)
                .issuedAt(new Date(now))
                .expiration(new Date(now+expiry))
                .signWith(getSigningKey())
                .compact();
    }


    // Token Validation
    public boolean isTokenValid(String token){
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e){
            log.warn("JWT token is expired : {}", e.getMessage());
        } catch (UnsupportedJwtException e){
            log.warn("JWT token is unsupported : {}", e.getMessage());
        } catch (MalformedJwtException e){
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e){
            log.warn("JWT signature is invalid : {}", e.getMessage());
        } catch (IllegalArgumentException e){
            log.warn("JWT token is empty: {}", e.getMessage());
        }

        return false;
    }


    // Claims Extraction

    public String extractUserUuid(String token){
        return extractClaim(token,Claims::getSubject);
    }

    public String extractTokenId(String token){
        return extractClaim(token,Claims::getId);
    }

    public Date extractExpiration(String token){
        return extractClaim(token,Claims::getExpiration);
    }


    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }


    // Internal
    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
