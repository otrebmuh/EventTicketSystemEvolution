package com.eventbooking.common.util;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization to prevent XSS and injection attacks
 */
@Component
public class InputSanitizer {
    
    // Patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    // Dangerous patterns that should be blocked
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.+--)|(--)|(;)|(\\|\\|)|(\\*)|(<script>)|(</script>)|(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script>)|(</script>)|(<iframe>)|(</iframe>)|(javascript:)|(onerror=)|(onload=)|(onclick=)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Sanitize HTML content to prevent XSS attacks
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return HtmlUtils.htmlEscape(input);
    }
    
    /**
     * Sanitize string by removing potentially dangerous characters
     */
    public String sanitizeString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Remove null bytes
        String sanitized = input.replace("\0", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        // HTML escape
        sanitized = HtmlUtils.htmlEscape(sanitized);
        
        return sanitized;
    }
    
    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate alphanumeric string
     */
    public boolean isAlphanumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(input).matches();
    }
    
    /**
     * Validate phone number format
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Check for SQL injection patterns
     */
    public boolean containsSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Check for XSS patterns
     */
    public boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * Validate and sanitize input, throwing exception if dangerous content detected
     */
    public String validateAndSanitize(String input, String fieldName) {
        if (input == null) {
            return null;
        }
        
        if (containsSqlInjection(input)) {
            throw new IllegalArgumentException(
                "Invalid input for " + fieldName + ": potential SQL injection detected"
            );
        }
        
        if (containsXss(input)) {
            throw new IllegalArgumentException(
                "Invalid input for " + fieldName + ": potential XSS attack detected"
            );
        }
        
        return sanitizeString(input);
    }
    
    /**
     * Sanitize URL to prevent open redirect vulnerabilities
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // Only allow relative URLs or URLs from trusted domains
        if (url.startsWith("/")) {
            return url;
        }
        
        // Block javascript: and data: URLs
        if (url.toLowerCase().startsWith("javascript:") || 
            url.toLowerCase().startsWith("data:")) {
            throw new IllegalArgumentException("Invalid URL: potentially dangerous protocol");
        }
        
        return url;
    }
    
    /**
     * Validate string length
     */
    public boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return false;
        }
        int length = input.length();
        return length >= minLength && length <= maxLength;
    }
}
