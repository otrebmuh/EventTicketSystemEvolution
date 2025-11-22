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
 * Security tests for request validation functionality
 * Tests Requirements: 9.1-9.5 (Security requirements)
 */
@ExtendWith(MockitoExtension.class)
class RequestValidationFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private RequestValidationFilter requestValidationFilter;
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws Exception {
        requestValidationFilter = new RequestValidationFilter();
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }
    
    @Test
    void testValidGetRequestPassesThrough() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getContentLengthLong()).thenReturn(0L);
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testValidPostRequestPassesThrough() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getContentLengthLong()).thenReturn(1024L);
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testInvalidHttpMethodIsBlocked() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("TRACE");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(405);
        verify(filterChain, never()).doFilter(request, response);
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("METHOD_NOT_ALLOWED"));
    }
    
    @Test
    void testRequestTooLargeIsBlocked() throws Exception {
        // Given - Request larger than 10MB
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getContentLengthLong()).thenReturn(11L * 1024 * 1024); // 11MB
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(413);
        verify(filterChain, never()).doFilter(request, response);
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("PAYLOAD_TOO_LARGE"));
    }
    
    @Test
    void testInvalidContentTypeIsBlocked() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getContentType()).thenReturn("application/xml");
        when(request.getContentLengthLong()).thenReturn(1024L);
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(415);
        verify(filterChain, never()).doFilter(request, response);
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("UNSUPPORTED_MEDIA_TYPE"));
    }
    
    @Test
    void testValidContentTypesAreAllowed() throws Exception {
        // Test application/json
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getContentLengthLong()).thenReturn(1024L);
        
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        
        // Test multipart/form-data
        reset(filterChain);
        when(request.getContentType()).thenReturn("multipart/form-data");
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        
        // Test application/x-www-form-urlencoded
        reset(filterChain);
        when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testPathTraversalAttackIsBlocked() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events/../../../etc/passwd");
        when(request.getContentLengthLong()).thenReturn(0L);
        when(request.getRemoteAddr()).thenReturn("192.168.1.4");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(400);
        verify(filterChain, never()).doFilter(request, response);
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("BAD_REQUEST"));
    }
    
    @Test
    void testNullByteInjectionIsBlocked() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events%00.jpg");
        when(request.getContentLengthLong()).thenReturn(0L);
        when(request.getRemoteAddr()).thenReturn("192.168.1.5");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(400);
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void testScriptInjectionIsBlocked() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events?name=<script>alert('xss')</script>");
        when(request.getContentLengthLong()).thenReturn(0L);
        when(request.getRemoteAddr()).thenReturn("192.168.1.6");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(400);
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void testLocalhostOriginIsAllowed() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("Origin")).thenReturn("http://localhost:3000");
        when(request.getContentLengthLong()).thenReturn(0L);
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testProductionOriginIsAllowed() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("Origin")).thenReturn("https://app.eventbooking.com");
        when(request.getContentLengthLong()).thenReturn(0L);
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testXForwardedForIsUsedForClientIp() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("TRACE");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.7");
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then - Should log with first IP from X-Forwarded-For
        verify(response).setStatus(405);
    }
    
    @Test
    void testContentTypeWithCharsetIsHandled() throws Exception {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getContentType()).thenReturn("application/json; charset=UTF-8");
        when(request.getContentLengthLong()).thenReturn(1024L);
        
        // When
        requestValidationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
}
