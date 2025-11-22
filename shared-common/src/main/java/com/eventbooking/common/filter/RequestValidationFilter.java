package com.eventbooking.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to validate incoming requests for security threats
 */
@Component
public class RequestValidationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestValidationFilter.class);
    
    // Allowed HTTP methods
    private static final List<String> ALLOWED_METHODS = Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    );
    
    // Maximum request size (10MB)
    private static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Validate HTTP method
        if (!ALLOWED_METHODS.contains(request.getMethod())) {
            log.warn("Invalid HTTP method: {} from IP: {}", 
                request.getMethod(), getClientIp(request));
            sendErrorResponse(response, 405, "METHOD_NOT_ALLOWED", 
                "HTTP method not allowed");
            return;
        }
        
        // Validate Content-Length
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_REQUEST_SIZE) {
            log.warn("Request size too large: {} bytes from IP: {}", 
                contentLength, getClientIp(request));
            sendErrorResponse(response, 413, "PAYLOAD_TOO_LARGE", 
                "Request payload too large");
            return;
        }
        
        // Validate Content-Type for POST/PUT/PATCH requests
        if (Arrays.asList("POST", "PUT", "PATCH").contains(request.getMethod())) {
            String contentType = request.getContentType();
            if (contentType != null && !isValidContentType(contentType)) {
                log.warn("Invalid Content-Type: {} from IP: {}", 
                    contentType, getClientIp(request));
                sendErrorResponse(response, 415, "UNSUPPORTED_MEDIA_TYPE", 
                    "Unsupported media type");
                return;
            }
        }
        
        // Validate Origin header for CORS requests
        String origin = request.getHeader("Origin");
        if (origin != null && !isValidOrigin(origin)) {
            log.warn("Invalid Origin: {} from IP: {}", origin, getClientIp(request));
            sendErrorResponse(response, 403, "FORBIDDEN", 
                "Invalid origin");
            return;
        }
        
        // Check for suspicious patterns in URI
        String uri = request.getRequestURI();
        if (containsSuspiciousPatterns(uri)) {
            log.warn("Suspicious URI pattern detected: {} from IP: {}", 
                uri, getClientIp(request));
            sendErrorResponse(response, 400, "BAD_REQUEST", 
                "Invalid request");
            return;
        }
        
        // Validate User-Agent (block empty or suspicious user agents)
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty()) {
            log.warn("Missing User-Agent from IP: {}", getClientIp(request));
            // Don't block, but log for monitoring
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if Content-Type is valid
     */
    private boolean isValidContentType(String contentType) {
        String baseType = contentType.split(";")[0].trim().toLowerCase();
        return baseType.equals("application/json") ||
               baseType.equals("application/x-www-form-urlencoded") ||
               baseType.equals("multipart/form-data") ||
               baseType.equals("text/plain");
    }
    
    /**
     * Check if Origin is valid (should be configured per environment)
     */
    private boolean isValidOrigin(String origin) {
        // Allow localhost for development
        if (origin.startsWith("http://localhost") || 
            origin.startsWith("https://localhost")) {
            return true;
        }
        
        // Allow production domains
        if (origin.endsWith(".eventbooking.com")) {
            return true;
        }
        
        // For development, allow all origins
        // In production, this should be strictly controlled
        return true;
    }
    
    /**
     * Check for suspicious patterns in URI
     */
    private boolean containsSuspiciousPatterns(String uri) {
        if (uri == null) {
            return false;
        }
        
        String lowerUri = uri.toLowerCase();
        
        // Check for path traversal attempts
        if (lowerUri.contains("../") || lowerUri.contains("..\\")) {
            return true;
        }
        
        // Check for null byte injection
        if (lowerUri.contains("%00")) {
            return true;
        }
        
        // Check for script injection attempts
        if (lowerUri.contains("<script") || lowerUri.contains("javascript:")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status, 
                                   String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":{" +
            "\"code\":\"" + code + "\"," +
            "\"message\":\"" + message + "\"," +
            "\"timestamp\":\"" + java.time.Instant.now() + "\"" +
            "}}"
        );
    }
}
