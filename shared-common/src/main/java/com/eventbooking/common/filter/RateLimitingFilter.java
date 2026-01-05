package com.eventbooking.common.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent abuse and DDoS attacks
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    // Rate limit configurations for different endpoint types
    private static final int DEFAULT_LIMIT_FOR_PERIOD = 100; // requests
    private static final Duration DEFAULT_LIMIT_REFRESH_PERIOD = Duration.ofMinutes(1);
    private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofMillis(100);
    
    // Stricter limits for sensitive endpoints
    private static final int AUTH_LIMIT_FOR_PERIOD = 5; // requests
    private static final Duration AUTH_LIMIT_REFRESH_PERIOD = Duration.ofMinutes(1);
    
    // More permissive limits for password reset at filter level (real control is in service)
    private static final int PASSWORD_RESET_LIMIT = 10; // requests per hour (service controls the real limit of 3)
    private static final Duration PASSWORD_RESET_PERIOD = Duration.ofHours(1);
    
    public RateLimitingFilter() {
        this.rateLimiterRegistry = RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitForPeriod(DEFAULT_LIMIT_FOR_PERIOD)
                .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
                .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                .build()
        );
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = getClientIdentifier(request);
        String path = request.getRequestURI();
        
        // Determine rate limit configuration based on endpoint
        RateLimiter rateLimiter = getRateLimiterForEndpoint(clientId, path);
        
        try {
            // Attempt to acquire permission
            if (!rateLimiter.acquirePermission()) {
                log.warn("Rate limit exceeded for client: {} on endpoint: {} (Filter level)", clientId, path);
                handleRateLimitExceeded(response);
                return;
            }
            
            // Add rate limit headers to response
            addRateLimitHeaders(response, rateLimiter);
            
            // Log successful rate limit check for password reset endpoints
            if (path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
                RateLimiter.Metrics metrics = rateLimiter.getMetrics();
                log.debug("Password reset rate limit check passed for client: {} - Available permissions: {}", 
                    clientId, metrics.getAvailablePermissions());
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // Continue processing even if rate limiting fails
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Get client identifier from request (IP address or user ID)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // For password reset endpoints, try to use email from request body
        String path = request.getRequestURI();
        if (path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
            // For password reset, we want to rate limit by email, not IP
            // This will be handled at the service level, so use a more permissive approach here
            return "password-reset:" + getClientIpAddress(request);
        }
        
        // Try to get user ID from request attribute (set by authentication filter)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        return "ip:" + getClientIpAddress(request);
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
    
    /**
     * Get or create rate limiter for specific endpoint and client
     */
    private RateLimiter getRateLimiterForEndpoint(String clientId, String path) {
        String key = clientId + ":" + path;
        
        return rateLimiters.computeIfAbsent(key, k -> {
            RateLimiterConfig config = getRateLimitConfig(path);
            return rateLimiterRegistry.rateLimiter(k, config);
        });
    }
    
    /**
     * Get rate limit configuration based on endpoint path
     */
    private RateLimiterConfig getRateLimitConfig(String path) {
        // Stricter limits for authentication endpoints
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            return RateLimiterConfig.custom()
                .limitForPeriod(AUTH_LIMIT_FOR_PERIOD)
                .limitRefreshPeriod(AUTH_LIMIT_REFRESH_PERIOD)
                .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                .build();
        }
        
        // Very strict limits for password reset
        if (path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
            return RateLimiterConfig.custom()
                .limitForPeriod(PASSWORD_RESET_LIMIT)
                .limitRefreshPeriod(PASSWORD_RESET_PERIOD)
                .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                .build();
        }
        
        // Moderate limits for payment endpoints
        if (path.contains("/payment")) {
            return RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                .build();
        }
        
        // Default limits for other endpoints
        return RateLimiterConfig.custom()
            .limitForPeriod(DEFAULT_LIMIT_FOR_PERIOD)
            .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
            .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
            .build();
    }
    
    /**
     * Handle rate limit exceeded response
     */
    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":{" +
            "\"code\":\"RATE_LIMIT_EXCEEDED\"," +
            "\"message\":\"Too many requests. Please try again later.\"," +
            "\"timestamp\":\"" + java.time.Instant.now() + "\"" +
            "}}"
        );
    }
    
    /**
     * Add rate limit information to response headers
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimiter rateLimiter) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        response.setHeader("X-RateLimit-Limit", 
            String.valueOf(rateLimiter.getRateLimiterConfig().getLimitForPeriod()));
        response.setHeader("X-RateLimit-Remaining", 
            String.valueOf(metrics.getAvailablePermissions()));
    }
}
