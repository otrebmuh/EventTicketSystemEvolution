package com.eventbooking.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Security tests for rate limiting functionality
 * Tests Requirements: 9.1-9.5 (Security requirements)
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private RateLimitingFilter rateLimitingFilter;
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws Exception {
        rateLimitingFilter = new RateLimitingFilter();
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }
    
    @Test
    void testNormalRequestPassesThrough() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString());
    }
    
    @Test
    void testRateLimitExceededForAuthEndpoint() throws Exception {
        // Given - Auth endpoints have strict limit of 5 requests per minute
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        
        // When - Make 6 requests rapidly
        for (int i = 0; i < 6; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Then - 6th request should be rate limited
        verify(response, atLeastOnce()).setStatus(429);
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("RATE_LIMIT_EXCEEDED"));
        assertTrue(responseContent.contains("Too many requests"));
    }
    
    @Test
    void testRateLimitExceededForPasswordReset() throws Exception {
        // Given - Password reset has limit of 3 requests per hour
        when(request.getRequestURI()).thenReturn("/api/auth/forgot-password");
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");
        
        // When - Make 4 requests rapidly
        for (int i = 0; i < 4; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Then - 4th request should be rate limited
        verify(response, atLeastOnce()).setStatus(429);
    }
    
    @Test
    void testDifferentClientsHaveSeparateLimits() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        
        // When - Client 1 makes 5 requests
        when(request.getRemoteAddr()).thenReturn("192.168.1.4");
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // And - Client 2 makes a request
        when(request.getRemoteAddr()).thenReturn("192.168.1.5");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then - Client 2's request should pass through
        verify(filterChain, times(6)).doFilter(request, response);
    }
    
    @Test
    void testUserIdBasedRateLimiting() throws Exception {
        // Given - Authenticated user
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getAttribute("userId")).thenReturn("user-123");
        when(request.getRemoteAddr()).thenReturn("192.168.1.6");
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then - Should use user ID for rate limiting
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testXForwardedForHeaderUsed() throws Exception {
        // Given - Request through proxy
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.7");
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then - Should use first IP from X-Forwarded-For
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testPaymentEndpointHasModerateLimit() throws Exception {
        // Given - Payment endpoint has limit of 20 requests per minute
        when(request.getRequestURI()).thenReturn("/api/payment/process");
        when(request.getRemoteAddr()).thenReturn("192.168.1.8");
        
        // When - Make 21 requests rapidly
        for (int i = 0; i < 21; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Then - 21st request should be rate limited
        verify(response, atLeastOnce()).setStatus(429);
    }
    
    @Test
    void testRateLimitHeadersAreSet() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getRemoteAddr()).thenReturn("192.168.1.9");
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setHeader(eq("X-RateLimit-Limit"), anyString());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString());
    }
    
    @Test
    void testErrorInFilterDoesNotBlockRequest() throws Exception {
        // Given - Simulate error condition
        when(request.getRequestURI()).thenThrow(new RuntimeException("Test error"));
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then - Request should still pass through
        verify(filterChain).doFilter(request, response);
    }
}
