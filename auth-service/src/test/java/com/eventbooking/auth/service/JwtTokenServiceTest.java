package com.eventbooking.auth.service;

import com.eventbooking.auth.config.JwtConfig;
import com.eventbooking.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create real JWT config for testing
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-that-is-long-enough-for-hmac-sha-256-algorithm");
        jwtConfig.setExpiration(3600000L); // 1 hour
        jwtConfig.setRefreshExpiration(604800000L); // 7 days
        jwtConfig.setIssuer("event-booking-system");
        jwtConfig.setAudience("event-booking-client");

        jwtTokenService = new JwtTokenService(jwtConfig);

        // Create test user
        testUser = new User(
            "test@example.com",
            "hashedPassword",
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1)
        );
        testUser.setId(UUID.randomUUID());
        testUser.setEmailVerified(true);
    }

    @Test
    void generateToken_ShouldGenerateValidToken() {
        String token = jwtTokenService.generateToken(testUser, false);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        String extractedEmail = jwtTokenService.extractUsername(token);
        UUID extractedUserId = jwtTokenService.extractUserId(token);
        
        assertEquals(testUser.getEmail(), extractedEmail);
        assertEquals(testUser.getId(), extractedUserId);
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnEmail() {
        String token = jwtTokenService.generateToken(testUser, false);

        String extractedEmail = jwtTokenService.extractUsername(token);

        assertEquals(testUser.getEmail(), extractedEmail);
    }

    @Test
    void extractUserId_WithValidToken_ShouldReturnUserId() {
        String token = jwtTokenService.generateToken(testUser, false);

        UUID extractedUserId = jwtTokenService.extractUserId(token);

        assertEquals(testUser.getId(), extractedUserId);
    }

    @Test
    void extractEmailVerified_WithValidToken_ShouldReturnEmailVerifiedStatus() {
        String token = jwtTokenService.generateToken(testUser, false);

        Boolean emailVerified = jwtTokenService.extractEmailVerified(token);

        assertTrue(emailVerified);
    }

    @Test
    void extractTokenType_WithAccessToken_ShouldReturnAccessType() {
        String token = jwtTokenService.generateToken(testUser, false);

        String tokenType = jwtTokenService.extractTokenType(token);

        assertEquals("access", tokenType);
    }

    @Test
    void generateRefreshToken_ShouldGenerateValidRefreshToken() {
        String refreshToken = jwtTokenService.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        
        String tokenType = jwtTokenService.extractTokenType(refreshToken);
        assertEquals("refresh", tokenType);
        
        String extractedEmail = jwtTokenService.extractUsername(refreshToken);
        assertEquals(testUser.getEmail(), extractedEmail);
    }

    @Test
    void isTokenExpired_WithValidToken_ShouldReturnFalse() {
        String token = jwtTokenService.generateToken(testUser, false);

        Boolean isExpired = jwtTokenService.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void validateToken_WithValidTokenAndMatchingUser_ShouldReturnTrue() {
        String token = jwtTokenService.generateToken(testUser, false);

        Boolean isValid = jwtTokenService.validateToken(token, testUser);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithValidTokenAndDifferentUser_ShouldReturnFalse() {
        String token = jwtTokenService.generateToken(testUser, false);
        
        User differentUser = new User(
            "different@example.com",
            "hashedPassword",
            "Jane",
            "Smith",
            LocalDate.of(1992, 5, 15)
        );
        differentUser.setId(UUID.randomUUID());
        differentUser.setEmailVerified(true);

        Boolean isValid = jwtTokenService.validateToken(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    void validateTokenStructure_WithValidToken_ShouldReturnTrue() {
        String token = jwtTokenService.generateToken(testUser, false);

        Boolean isValid = jwtTokenService.validateTokenStructure(token);

        assertTrue(isValid);
    }

    @Test
    void validateTokenStructure_WithInvalidToken_ShouldReturnFalse() {
        String invalidToken = "invalid.token.here";

        Boolean isValid = jwtTokenService.validateTokenStructure(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void getTokenExpirationInSeconds_WithRememberMeFalse_ShouldReturnShortExpiration() {
        long expirationSeconds = jwtTokenService.getTokenExpirationInSeconds(false);

        assertEquals(3600L, expirationSeconds); // 1 hour
    }

    @Test
    void getTokenExpirationInSeconds_WithRememberMeTrue_ShouldReturnLongExpiration() {
        long expirationSeconds = jwtTokenService.getTokenExpirationInSeconds(true);

        assertEquals(604800L, expirationSeconds); // 7 days
    }
}
