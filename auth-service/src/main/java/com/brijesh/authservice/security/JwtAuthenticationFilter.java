package com.brijesh.authservice.security;


import com.brijesh.authservice.infrastructure.redis.RedisTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
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
        String userUuid = jwtTokenProvider.extractUserUuid(token);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUuid(userUuid);

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
        log.debug("Authentication user : {}", userUuid);

        filterChain.doFilter(request,response);
    }


    private String extractTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remove "Bearer" Prefix
        }
        return null;
    }
}
