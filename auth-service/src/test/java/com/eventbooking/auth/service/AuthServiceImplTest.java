package com.eventbooking.auth.service;

import com.eventbooking.auth.dto.*;
import com.eventbooking.auth.entity.EmailVerificationToken;
import com.eventbooking.auth.entity.PasswordResetToken;
import com.eventbooking.auth.entity.User;
import com.eventbooking.auth.entity.UserSession;
import com.eventbooking.auth.model.RedisUserSession;
import com.eventbooking.auth.repository.*;
import com.eventbooking.common.dto.UserDto;
import com.eventbooking.common.exception.UnauthorizedException;
import com.eventbooking.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private RedisUserSessionRepository redisUserSessionRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        testUserId = UUID.randomUUID();
        
        testUser = new User(
            "test@example.com",
            passwordEncoder.encode("SecurePass123!"),
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1)
        );
        testUser.setId(testUserId);
        testUser.setEmailVerified(true);
    }

    // ========== User Registration Tests ==========

    @Test
    void registerUser_WithValidData_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setConfirmPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
            .thenReturn(new EmailVerificationToken(testUserId, "token", LocalDateTime.now().plusHours(24)));

        UserDto result = authService.registerUser(request, "IP: 127.0.0.1, User-Agent: Test");

        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowValidationException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("SecurePass123!");
        request.setConfirmPassword("SecurePass123!");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ValidationException.class, () -> 
            authService.registerUser(request, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    @Test
    void registerUser_WithMismatchedPasswords_ShouldThrowValidationException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setConfirmPassword("DifferentPass123!");

        assertThrows(ValidationException.class, () -> 
            authService.registerUser(request, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    // ========== Email Verification Tests ==========

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyUser() {
        String token = "valid-token";
        User unverifiedUser = new User("test@example.com", "hash", "John", "Doe", LocalDate.of(1990, 1, 1));
        unverifiedUser.setId(testUserId);
        unverifiedUser.setEmailVerified(false);

        EmailVerificationToken verificationToken = new EmailVerificationToken(
            testUserId, token, LocalDateTime.now().plusHours(24)
        );

        when(emailVerificationTokenRepository.findValidTokenByToken(eq(token), any(LocalDateTime.class)))
            .thenReturn(Optional.of(verificationToken));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(unverifiedUser));
        when(userRepository.save(any(User.class))).thenReturn(unverifiedUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class))).thenReturn(verificationToken);

        authService.verifyEmail(token);

        assertTrue(unverifiedUser.isEmailVerified());
        verify(userRepository).save(unverifiedUser);
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowValidationException() {
        String token = "invalid-token";

        when(emailVerificationTokenRepository.findValidTokenByToken(eq(token), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.verifyEmail(token));
    }

    // ========== User Login Tests ==========

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setRememberMe(false);

        String jwtToken = "jwt-token";
        long expiresIn = 3600L;

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtTokenService.generateToken(testUser, false)).thenReturn(jwtToken);
        when(jwtTokenService.getTokenExpirationInSeconds(false)).thenReturn(expiresIn);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> {
            UserSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });
        when(redisUserSessionRepository.save(any(RedisUserSession.class))).thenReturn(new RedisUserSession());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        LoginResponse response = authService.authenticateUser(request, "IP: 127.0.0.1, User-Agent: Test");

        assertNotNull(response);
        assertEquals(jwtToken, response.getAccessToken());
        assertEquals(expiresIn, response.getExpiresIn());
        verify(jwtTokenService).generateToken(testUser, false);
    }

    @Test
    void authenticateUser_WithInvalidEmail_ShouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("SecurePass123!");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> 
            authService.authenticateUser(request, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    @Test
    void authenticateUser_WithInvalidPassword_ShouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword123!");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(UnauthorizedException.class, () -> 
            authService.authenticateUser(request, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    @Test
    void authenticateUser_WithUnverifiedEmail_ShouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");

        User unverifiedUser = new User(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1)
        );
        unverifiedUser.setId(testUserId);
        unverifiedUser.setEmailVerified(false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        assertThrows(UnauthorizedException.class, () -> 
            authService.authenticateUser(request, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    // ========== Password Reset Tests ==========

    @Test
    void initiatePasswordReset_WithValidEmail_ShouldSendResetEmail() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countRecentRequestsByUserId(eq(testUserId), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenReturn(new PasswordResetToken());

        authService.initiatePasswordReset(email, "IP: 127.0.0.1, User-Agent: Test");

        verify(passwordResetTokenRepository).invalidateAllUserTokens(testUserId);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(email), eq("John"), anyString());
    }

    @Test
    void initiatePasswordReset_WithExceededRateLimit_ShouldThrowValidationException() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.countRecentRequestsByUserId(eq(testUserId), any(LocalDateTime.class)))
            .thenReturn(3L);

        assertThrows(ValidationException.class, () -> 
            authService.initiatePasswordReset(email, "IP: 127.0.0.1, User-Agent: Test")
        );
    }

    @Test
    void resetPassword_WithValidToken_ShouldUpdatePassword() {
        String token = "valid-reset-token";
        String newPassword = "NewSecurePass123!";

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(newPassword);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUserId, token, LocalDateTime.now().plusMinutes(15), "127.0.0.1", "Test Agent"
        );

        when(passwordResetTokenRepository.findValidTokenByToken(eq(token), any(LocalDateTime.class)))
            .thenReturn(Optional.of(resetToken));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        authService.resetPassword(request);

        verify(userRepository).save(testUser);
        verify(userSessionRepository).deactivateAllUserSessions(testUserId);
        verify(emailService).sendPasswordChangeConfirmation(eq("test@example.com"), eq("John"));
    }

    @Test
    void resetPassword_WithInvalidToken_ShouldThrowValidationException() {
        String token = "invalid-token";
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword("NewSecurePass123!");
        request.setConfirmPassword("NewSecurePass123!");

        when(passwordResetTokenRepository.findValidTokenByToken(eq(token), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.resetPassword(request));
    }

    // ========== JWT Token Handling Tests ==========

    @Test
    void validateTokenAndGetUser_WithValidToken_ShouldReturnUser() {
        String token = "valid-jwt-token";

        when(jwtTokenService.validateTokenStructure(token)).thenReturn(true);
        when(jwtTokenService.extractUserId(token)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtTokenService.validateToken(token, testUser)).thenReturn(true);

        UserDto result = authService.validateTokenAndGetUser(token);

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(jwtTokenService).validateToken(token, testUser);
    }

    @Test
    void validateTokenAndGetUser_WithInvalidToken_ShouldThrowUnauthorizedException() {
        String token = "invalid-token";

        when(jwtTokenService.validateTokenStructure(token)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> 
            authService.validateTokenAndGetUser(token)
        );
    }

    @Test
    void logoutUser_WithValidToken_ShouldDeactivateSessions() {
        String token = "valid-jwt-token";
        String tokenHash = String.valueOf(token.hashCode());

        RedisUserSession redisSession = new RedisUserSession();
        when(redisUserSessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(redisSession));

        authService.logoutUser(token);

        verify(userSessionRepository).deactivateSessionByTokenHash(tokenHash);
        verify(redisUserSessionRepository).delete(redisSession);
    }
}
