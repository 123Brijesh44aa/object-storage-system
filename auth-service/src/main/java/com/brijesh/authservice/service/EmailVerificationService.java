package com.brijesh.authservice.service;


import com.brijesh.authservice.domain.entity.EmailVerificationToken;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.infrastructure.email.EmailService;
import com.brijesh.authservice.repository.EmailVerificationTokenRepository;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.security.TokenHashUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final long TOKEN_EXPIRY_HOURS = 24;

    // Send verification Email

    @Transactional
    public void sendVerificationEmail(User user){

        // Invalidate any previous unused tokens for this user
        tokenRepository.markAllAsUsedByUserId(user.getId());

        // Generate raw token - this is what goes in the email
        String rawToken = UUID.randomUUID().toString();

        // store only the hash - never the raw token
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.hash(rawToken))
                .expiresAt(Instant.now().plusSeconds(TOKEN_EXPIRY_HOURS * 3600))
                .build();

        tokenRepository.save(token);

        // Send raw token in email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFirstName(),
                rawToken
        );

        log.info("Verification email sent to : {} ", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String rawToken){
        // Hash the incoming token to look it up in DB
        String hash = TokenHashUtil.hash(rawToken);

        EmailVerificationToken token = tokenRepository.findEmailVerificationTokenByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid verification token"));

        if (token.getUsed()) {
            throw new AuthException("Verification token already used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("Verification token has expired");
        }

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        // Enable the user's account
         User user = token.getUser();
         user.setIsEnabled(true);
         userRepository.save(user);

         log.info("Email verified for user : {} ", user.getEmail());
    }
}
