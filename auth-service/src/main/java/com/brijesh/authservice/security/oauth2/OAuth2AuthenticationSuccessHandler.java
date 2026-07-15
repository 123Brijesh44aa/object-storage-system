package com.brijesh.authservice.security.oauth2;

import com.brijesh.authservice.config.AppProperties;
import com.brijesh.authservice.security.CustomUserDetails;
import com.brijesh.authservice.security.JwtTokenProvider;
import com.brijesh.authservice.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Issue OUR JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Save refresh token
        Instant refreshExpiry = Instant.now().plusMillis(appProperties.getJwt().getRefreshTokenExpiry());

        refreshTokenService.saveRefreshToken(
                userDetails.getUser(),
                refreshToken,
                refreshExpiry,
                getClientIp(request),
                request.getHeader("User-Agent")
        );

        log.info("OAuth2 login successful for: {}", userDetails.getEmail());

        // Return tokens as JSON
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "accessToken", accessToken,
                "refreshToken",refreshToken,
                "tokenType","Bearer",
                "expiresIn", appProperties.getJwt().getAccessTokenExpiry()
        )));
    }

    private String getClientIp(HttpServletRequest request){
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()){
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}





















