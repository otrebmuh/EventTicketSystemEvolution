package com.eventbooking.common.util;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating and validating CSRF tokens
 * Note: For JWT-based stateless APIs, CSRF protection is primarily handled through:
 * 1. SameSite cookie attributes
 * 2. Custom headers (X-Requested-With)
 * 3. Origin/Referer validation
 */
@Service
public class CsrfTokenService {
    
    private static final int TOKEN_LENGTH = 32;
    private static final long TOKEN_VALIDITY_MS = TimeUnit.HOURS.toMillis(1);
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * Generate a new CSRF token for a session
     */
    public String generateToken(String sessionId) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        tokenStore.put(sessionId, new TokenData(token, System.currentTimeMillis()));
        
        // Clean up expired tokens
        cleanupExpiredTokens();
        
        return token;
    }
    
    /**
     * Validate CSRF token
     */
    public boolean validateToken(String sessionId, String token) {
        if (sessionId == null || token == null) {
            return false;
        }
        
        TokenData tokenData = tokenStore.get(sessionId);
        if (tokenData == null) {
            return false;
        }
        
        // Check if token is expired
        if (System.currentTimeMillis() - tokenData.timestamp > TOKEN_VALIDITY_MS) {
            tokenStore.remove(sessionId);
            return false;
        }
        
        return token.equals(tokenData.token);
    }
    
    /**
     * Invalidate token for a session
     */
    public void invalidateToken(String sessionId) {
        tokenStore.remove(sessionId);
    }
    
    /**
     * Clean up expired tokens
     */
    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        tokenStore.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > TOKEN_VALIDITY_MS
        );
    }
    
    private static class TokenData {
        final String token;
        final long timestamp;
        
        TokenData(String token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }
    }
}
