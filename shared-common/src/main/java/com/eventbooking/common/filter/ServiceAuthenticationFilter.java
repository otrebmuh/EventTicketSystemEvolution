package com.eventbooking.common.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;

@Component
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthenticationFilter.class);

    @Value("${service.auth.secret:defaultServiceSecret123456789012345678901234567890}")
    private String serviceSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only validate service token for internal endpoints
        if (path.contains("/internal/")) {
            String serviceToken = request.getHeader("X-Service-Token");
            String serviceName = request.getHeader("X-Service-Name");

            if (serviceToken == null || serviceName == null) {
                log.warn("Missing service authentication headers for internal endpoint: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Missing service authentication\"}");
                return;
            }

            if (!validateServiceToken(serviceToken)) {
                log.warn("Invalid service token from service: {} for endpoint: {}", serviceName, path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid service token\"}");
                return;
            }

            // Add service name to request attributes for logging/auditing
            request.setAttribute("serviceName", serviceName);
            log.debug("Validated service token from: {}", serviceName);
        }

        filterChain.doFilter(request, response);
    }

    private boolean validateServiceToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(serviceSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Verify it's a service token
            String type = claims.get("type", String.class);
            return "SERVICE".equals(type);
        } catch (Exception e) {
            log.error("Failed to validate service token", e);
            return false;
        }
    }
}
