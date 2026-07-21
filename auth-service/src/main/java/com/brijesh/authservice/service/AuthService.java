package com.brijesh.authservice.service;


import com.brijesh.authservice.config.AppProperties;
import com.brijesh.authservice.domain.entity.Role;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.infrastructure.redis.RedisTokenService;
import com.brijesh.authservice.repository.RoleRepository;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.security.CustomUserDetails;
import com.brijesh.authservice.security.JwtTokenProvider;
import com.brijesh.authservice.web.dto.request.LoginRequest;
import com.brijesh.authservice.web.dto.request.RefreshTokenRequest;
import com.brijesh.authservice.web.dto.request.RegisterRequest;
import com.brijesh.authservice.web.dto.response.AuthResponse;
import com.brijesh.authservice.web.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;
    private final RefreshTokenService refreshTokenService;
    private final RedisTokenService redisTokenService;
    private final LoginAttemptService loginAttemptService;
    private final EmailVerificationService emailVerificationService;
    private final AuditService auditService;


    // Register
    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) throws AuthException {
        // 1. Check email isn't already taken
        if (userRepository.existsByEmail(request.getEmail())){
            throw new AuthException("Email already registered");
        }

        // 2. Load default role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new AuthException("Default role not found"));

        // 3. Build and save user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isEnabled(false)   // set false and verify email
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Send verification email asynchronously
        emailVerificationService.sendVerificationEmail(user);

        log.info("New user registered, verification email sent : {} ", user.getEmail());
        auditService.logRegistration(user.getUuid(),user.getEmail(),getClientIp(httpRequest));
    }


    // Login

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest){
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (loginAttemptService.isAccountLocked(user)){
            throw new AuthException("Account temporarily locked due to multiple failed login attempts. Try again later.");
        }

        try {
            // AuthenticationManager calls CustomUserDetailsService + Bcrypt verify
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Check email verification
            if (!userDetails.getUser().getIsEnabled()){
                throw new AuthException(
                        "Email not verified. Please check your inbox."
                );
            }

            loginAttemptService.recordSuccessfulLogin(user);

            log.info("User logged in: {}", userDetails.getEmail());
            auditService.logLogin(userDetails.getUuid(),userDetails.getEmail(),getClientIp(httpRequest));
            return buildAuthResponse(userDetails, httpRequest);
        } catch (BadCredentialsException ex){
            loginAttemptService.recordFailedAttempts(user);
            auditService.logFailedLogin(request.getEmail(),getClientIp(httpRequest),"Invalid credentials");
            throw new AuthException("Invalid Credentials");
        }
    }

    @Transactional
    public void resendVerification(String email){
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.getIsEnabled()){
                emailVerificationService.sendVerificationEmail(user);
            }
        });

        // No else - we don't reveal if email is registered or already verified
    }

    // Refresh

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest){
        String rawRefreshToken = request.getRefreshToken();

        // Validate the JWT signature/expiry first
        if (!jwtTokenProvider.isTokenValid(rawRefreshToken)){
            throw new AuthException("Invalid or expired refresh token");
        }

        // Validate against DB + rotate (revoke old, issue new)
        User user = refreshTokenService.validateAndRotate(rawRefreshToken);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        log.info("Token refreshed for user: {}", userDetails.getEmail());

        return buildAuthResponse(userDetails,httpRequest);
    }

    // Logout

    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletRequest httpServletRequest){
        String userUuid = jwtTokenProvider.extractUserUuid(accessToken);
        // 1. Blacklist the access token for its remaining lifetime
        if (jwtTokenProvider.isTokenValid(accessToken)){
            String jti = jwtTokenProvider.extractTokenId(accessToken);
            Date expiration = jwtTokenProvider.extractExpiration(accessToken);

            Duration remainingTtl = Duration.between(Instant.now(), expiration.toInstant());

            redisTokenService.blacklistToken(jti,remainingTtl);
        }

        // 2. Revoke the refresh token in DB
        if (refreshToken != null){
            refreshTokenService.revokeToken(refreshToken);
        }

        log.info("User logged out, tokens revoked");
        auditService.logLogout(userUuid,getClientIp(httpServletRequest));
    }


    private AuthResponse buildAuthResponse(CustomUserDetails userDetails, HttpServletRequest httpServletRequest){
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Persist refresh token (hashed) for rotation/revocation
        Instant refreshExpiry = Instant.now().plusMillis(appProperties.getJwt().getRefreshTokenExpiry());

        refreshTokenService.saveRefreshToken(
                userDetails.getUser(),
                refreshToken,
                refreshExpiry,
                getClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpiry())
                .user(UserResponse.builder()
                        .uuid(userDetails.getUuid())
                        .email(userDetails.getEmail())
                        .firstName(userDetails.getFirstName())
                        .lastName(userDetails.getLastName())
                        .roles(userDetails.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .filter(a -> a.startsWith("ROLE_"))
                                .collect(Collectors.toSet())
                        ).build())
                .build();
    }

    // get client ip address
    private String getClientIp(HttpServletRequest request){
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
