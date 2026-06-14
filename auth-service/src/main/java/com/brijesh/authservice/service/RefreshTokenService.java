package com.brijesh.authservice.service;

import com.brijesh.authservice.domain.entity.RefreshToken;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.repository.RefreshTokenRepository;
import com.brijesh.authservice.security.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Save a newly issued Refresh token (hashed).
    @Transactional
    public void saveRefreshToken(User user, String rawToken, Instant expiresAt, String ipAddress, String userAgent){
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.hash(rawToken))
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    // validate a refresh token, revoke it (rotation), return the associated user.
    // Throws if token is invalid, expired, or already revoked
    @Transactional
    public User validateAndRotate(String rawToken){
        String hash = TokenHashUtil.hash(rawToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (storedToken.getRevoked()) {
            log.warn("Attempted reuse of revoked refresh token for user{}", storedToken.getUser().getEmail());
            throw new AuthException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("Refresh token has expired");
        }

        // Rotation: revoke this token immediately - it can only be used once
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        return storedToken.getUser();
    }

    // Revoke a specific refresh token - used on logout.
    @Transactional
    public void revokeToken(String rawToken){
        String hash = TokenHashUtil.hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token ->{
            token.setRevoked(true);
           token.setRevokedAt(Instant.now());
           refreshTokenRepository.save(token);
        });
    }

}
