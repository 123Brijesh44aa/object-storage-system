package com.brijesh.authservice.service;

import com.brijesh.authservice.domain.entity.PasswordResetToken;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.infrastructure.kafka.KafkaEventPublisher;
import com.brijesh.authservice.infrastructure.redis.RedisTokenService;
import com.brijesh.authservice.repository.PasswordResetTokenRepository;
import com.brijesh.authservice.repository.RefreshTokenRepository;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.security.JwtTokenProvider;
import com.brijesh.authservice.security.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private static final long TOKEN_EXPIRY_HOURS = 1; // shorter than email verification
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenService redisTokenService;
    private final KafkaEventPublisher kafkaEventPublisher;

    // Forgot Password

    @Transactional
    public void forgotPassword(String email){
        // Always return without error - don't reveal if email exists
        userRepository.findByEmail(email).ifPresent(user -> {

            // Only send it if account is verified
            if (!user.getIsEnabled()) {
                return;                 // Silently skip unverified accounts
            }

            // Invalidate previous unused reset tokens
            passwordResetTokenRepository.markAllAsUsedByUserId(user.getId());

            // Generate and store token
            String rawToken = UUID.randomUUID().toString();

            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHashUtil.hash(rawToken))
                    .expiresAt(Instant.now().plusSeconds(TOKEN_EXPIRY_HOURS * 3600))
                    .build();

            passwordResetTokenRepository.save(token);

            // Publish event instead of sending email directly
            kafkaEventPublisher.publishPasswordResetRequested(
                    user.getUuid(),
                    user.getEmail(),
                    user.getFirstName(),
                    rawToken
            );

            log.info("Password reset event published for: {}", email);
        });
    }

    // Reset Password

    @Transactional
    public void resetPassword(String rawToken, String newPassword){

        String hash = TokenHashUtil.hash(rawToken);

        PasswordResetToken token = passwordResetTokenRepository.findPasswordResetTokenByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        if (token.getUsed()){
            throw new AuthException("Reset token has already been used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())){
            throw new AuthException("Reset token has expired");
        }

        User user = token.getUser();

        // Check new Password is different from current
        if (passwordEncoder.matches(newPassword,user.getPasswordHash())){
            throw new AuthException("New password must be different from current password");
        }

        // update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Revoke all refresh tokens - force fresh login on all devices
        // Password change = potential compromise = kill all sessions
        refreshTokenRepository.revokedAllByUserId(user.getId(),Instant.now());

        log.info("Password reset successful for user: {}",user.getEmail());
    }

    // Change Password (logged-in user)

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword, String currentAccessToken) {

        // Verify they know their current password
        if (!passwordEncoder.matches(currentPassword,user.getPasswordHash())){
            throw new AuthException("Current password is incorrect");
        }

        // Check new Password is different
        if (passwordEncoder.matches(newPassword,user.getPasswordHash())){
            throw new AuthException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens - same principle as reset
        refreshTokenRepository.revokedAllByUserId(user.getId(),Instant.now());

        // Blacklist current access token immediately
        if (currentAccessToken != null && jwtTokenProvider.isTokenValid(currentAccessToken)){
            String jti = jwtTokenProvider.extractTokenId(currentAccessToken);
            Date expiration = jwtTokenProvider.extractExpiration(currentAccessToken);
            Duration remainingTtl = Duration.between(
                    Instant.now(), expiration.toInstant());
            redisTokenService.blacklistToken(jti,remainingTtl);
        }

        log.info("Password changed for user: {}", user.getEmail());
    }


}




















