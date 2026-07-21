package com.brijesh.authservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // Unique ID for tracing this request across all logs
            String traceId = UUID.randomUUID().toString().substring(0,8);
            MDC.put("traceId", traceId);
            MDC.put("ip",getClientIp(request));
            MDC.put("method",request.getMethod());
            MDC.put("path", request.getRequestURI());

            // Add response header so frontend can trace requests too
            response.setHeader("X-Trace-ID", traceId);

            filterChain.doFilter(request,response);
        } finally {
            // Always clear MDC after request - previous memory leaks
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()){
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
