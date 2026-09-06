package com.brijesh.authservice.security;


import com.brijesh.authservice.infrastructure.redis.RedisTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;

    // Header names set by API Gateway
    private static final String HEADER_USER_UUID = "X-User-Uuid";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Path 1: Request came through API Gateway
        //  Gateway already validated JWT and set X-User-* headers
        String userUuid = request.getHeader(HEADER_USER_UUID);
        if (StringUtils.hasText(userUuid)){
            authenticateFromGatewayHeaders(request, userUuid);
            filterChain.doFilter(request, response);
            return;
        }


        // Path 2: Direct request (dev/testing without gateway) -
        // Validate JWT directly
        final String token = extractTokenFromRequest(request);

        // No token - pass request along as anonymous
        if (token == null){
            filterChain.doFilter(request,response);
            return;
        }

        // Invalid token - pass along, AuthorizationFilter will block if needed
        if (!jwtTokenProvider.isTokenValid(token)){
            filterChain.doFilter(request,response);
            return;
        }

        // check Redis blacklist
        String jti = jwtTokenProvider.extractTokenId(token);
        if (redisTokenService.isTokenBlacklisted(jti)){
            log.debug("Rejected blacklisted token jti={}", jti);
            filterChain.doFilter(request,response);
            return;
        }

        // Already authenticated in this request - skip
        if (SecurityContextHolder.getContext().getAuthentication() != null){
            filterChain.doFilter(request,response);
            return;
        }

        // Extract UUID from token, load user from DB
        String uuid = jwtTokenProvider.extractUserUuid(token);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUuid(uuid);

        // Build authentication object and put in SecurityContext
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,             // credentials null after authentication
                userDetails.getAuthorities()
        );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authentication user via JWT : {}", userUuid);

        filterChain.doFilter(request,response);
    }

    private void authenticateFromGatewayHeaders(HttpServletRequest request, String userUuid) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String rolesHeader = request.getHeader(HEADER_USER_ROLES);
        Collection<SimpleGrantedAuthority> authorities = Collections.emptyList();

        if (StringUtils.hasText(rolesHeader)) {
            // Roles come as: [ROLE_USER, user:read, storage:write]
            // Strip brackets and split by comma
            String cleaned = rolesHeader
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            authorities = Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }

        // Build authentication from gateway headers - no DB call needed
        var authentication = new UsernamePasswordAuthenticationToken(
                userUuid,    // principal - uuid (String, not UserDetails)
                null,
                authorities
        );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated user via Gateway headers: {}", userUuid);
    }


    private String extractTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remove "Bearer" Prefix
        }
        return null;
    }
}
