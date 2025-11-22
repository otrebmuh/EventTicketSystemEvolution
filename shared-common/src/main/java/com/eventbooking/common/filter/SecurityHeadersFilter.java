package com.eventbooking.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add security headers to all HTTP responses
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // X-Content-Type-Options: Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options: Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-XSS-Protection: Enable XSS filtering
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict-Transport-Security: Enforce HTTPS
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");
        
        // Content-Security-Policy: Restrict resource loading
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
        
        // Referrer-Policy: Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: Control browser features
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()");
        
        // Cache-Control: Prevent caching of sensitive data
        if (isSensitiveEndpoint(request.getRequestURI())) {
            response.setHeader("Cache-Control", 
                "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if endpoint contains sensitive data
     */
    private boolean isSensitiveEndpoint(String uri) {
        return uri.contains("/auth/") || 
               uri.contains("/payment/") || 
               uri.contains("/user/") ||
               uri.contains("/profile/");
    }
}
