package com.eventbooking.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {
    
    private InputSanitizer sanitizer;
    
    @BeforeEach
    void setUp() {
        sanitizer = new InputSanitizer();
    }
    
    @Test
    void testSanitizeHtml() {
        String input = "<script>alert('XSS')</script>";
        String sanitized = sanitizer.sanitizeHtml(input);
        
        assertFalse(sanitized.contains("<script>"));
        assertTrue(sanitized.contains("&lt;script&gt;"));
    }
    
    @Test
    void testIsValidEmail() {
        assertTrue(sanitizer.isValidEmail("user@example.com"));
        assertTrue(sanitizer.isValidEmail("user.name@example.co.uk"));
        
        assertFalse(sanitizer.isValidEmail("invalid"));
        assertFalse(sanitizer.isValidEmail("@example.com"));
        assertFalse(sanitizer.isValidEmail("user@"));
    }
    
    @Test
    void testContainsSqlInjection() {
        assertTrue(sanitizer.containsSqlInjection("'; DROP TABLE users--"));
        assertTrue(sanitizer.containsSqlInjection("1' OR '1'='1"));
        assertTrue(sanitizer.containsSqlInjection("SELECT * FROM users"));
        
        assertFalse(sanitizer.containsSqlInjection("normal text"));
        assertFalse(sanitizer.containsSqlInjection("user@example.com"));
    }
    
    @Test
    void testContainsXss() {
        assertTrue(sanitizer.containsXss("<script>alert('XSS')</script>"));
        assertTrue(sanitizer.containsXss("<iframe src='evil.com'></iframe>"));
        assertTrue(sanitizer.containsXss("javascript:alert('XSS')"));
        assertTrue(sanitizer.containsXss("<img onerror='alert(1)'>"));
        
        assertFalse(sanitizer.containsXss("normal text"));
        assertFalse(sanitizer.containsXss("user@example.com"));
    }
    
    @Test
    void testValidateAndSanitize() {
        String input = "  normal text  ";
        String result = sanitizer.validateAndSanitize(input, "test");
        
        assertEquals("normal text", result);
    }
    
    @Test
    void testValidateAndSanitizeThrowsOnXss() {
        String input = "<script>alert('XSS')</script>";
        
        assertThrows(IllegalArgumentException.class, () -> {
            sanitizer.validateAndSanitize(input, "test");
        });
    }
    
    @Test
    void testValidateAndSanitizeThrowsOnSqlInjection() {
        String input = "'; DROP TABLE users--";
        
        assertThrows(IllegalArgumentException.class, () -> {
            sanitizer.validateAndSanitize(input, "test");
        });
    }
    
    @Test
    void testSanitizeUrl() {
        assertEquals("/path/to/page", sanitizer.sanitizeUrl("/path/to/page"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            sanitizer.sanitizeUrl("javascript:alert('XSS')");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            sanitizer.sanitizeUrl("data:text/html,<script>alert('XSS')</script>");
        });
    }
    
    @Test
    void testIsValidLength() {
        assertTrue(sanitizer.isValidLength("test", 1, 10));
        assertTrue(sanitizer.isValidLength("test", 4, 4));
        
        assertFalse(sanitizer.isValidLength("test", 5, 10));
        assertFalse(sanitizer.isValidLength("test", 1, 3));
        assertFalse(sanitizer.isValidLength(null, 1, 10));
    }
    
    @Test
    void testIsValidPhone() {
        assertTrue(sanitizer.isValidPhone("+12345678901"));
        assertTrue(sanitizer.isValidPhone("12345678901"));
        
        assertFalse(sanitizer.isValidPhone("123"));
        assertFalse(sanitizer.isValidPhone("abc"));
        assertFalse(sanitizer.isValidPhone("+0123456789"));
    }
}
