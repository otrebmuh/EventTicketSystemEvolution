package com.eventbooking.auth.service;

import com.eventbooking.auth.dto.*;
import com.eventbooking.auth.entity.EmailVerificationToken;
import com.eventbooking.auth.entity.PasswordResetToken;
import com.eventbooking.auth.entity.User;
import com.eventbooking.auth.entity.UserSession;
import com.eventbooking.auth.model.RedisUserSession;
import com.eventbooking.auth.repository.EmailVerificationTokenRepository;
import com.eventbooking.auth.repository.PasswordResetTokenRepository;
import com.eventbooking.auth.repository.RedisUserSessionRepository;
import com.eventbooking.auth.repository.UserRepository;
import com.eventbooking.auth.repository.UserSessionRepository;
import com.eventbooking.common.dto.UserDto;
import com.eventbooking.common.exception.UnauthorizedException;
import com.eventbooking.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;
    private static final int EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    private static final int MAX_RESET_REQUESTS_PER_HOUR = 3;
    
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RedisUserSessionRepository redisUserSessionRepository;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                          UserSessionRepository userSessionRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          EmailVerificationTokenRepository emailVerificationTokenRepository,
                          RedisUserSessionRepository redisUserSessionRepository,
                          JwtTokenService jwtTokenService,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.redisUserSessionRepository = redisUserSessionRepository;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        
        logger.info("Auth service initialized - using hybrid session management (Database + Redis cache)");
    }
    
    @Override
    public UserDto registerUser(RegisterRequest request, String clientInfo) {
        logger.info("Registering user with email: {}", request.getEmail());
        
        // Validate password confirmation
        if (!request.isPasswordMatching()) {
            throw new ValidationException("Passwords do not match");
        }
        
        // Validate password complexity (additional validation beyond annotation)
        validatePasswordComplexity(request.getPassword());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("User with this email already exists");
        }
        
        // Create new user
        User user = new User(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getFirstName(),
            request.getLastName(),
            request.getDateOfBirth()
        );
        
        user = userRepository.save(user);
        
        // Create and send verification email
        try {
            String verificationToken = generateVerificationToken(user);
            emailService.sendEmailVerification(user.getEmail(), user.getFirstName(), verificationToken);
            logger.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email for user {}: {}", user.getEmail(), e.getMessage());
            // Don't fail registration if email sending fails
        }
        
        return convertToUserDto(user);
    }
    
    @Override
    public void verifyEmail(String token) {
        logger.info("Verifying email with token: {}", token);
        
        // Find valid verification token
        Optional<EmailVerificationToken> tokenOpt = emailVerificationTokenRepository
                .findValidTokenByToken(token, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Invalid or expired verification token");
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        Optional<User> userOpt = userRepository.findById(verificationToken.getUserId());
        
        if (userOpt.isEmpty()) {
            throw new ValidationException("User not found");
        }
        
        User user = userOpt.get();
        
        if (user.isEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }
        
        // Mark user as verified
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Mark token as used
        verificationToken.markAsUsed();
        emailVerificationTokenRepository.save(verificationToken);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
    }
    
    @Override
    public LoginResponse authenticateUser(LoginRequest request, String clientInfo) {
        logger.info("Authenticating user: {}", request.getEmail());
        
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid email or password");
        }
        
        User user = userOpt.get();
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            LocalDateTime unlockTime = user.getLastLoginAttempt().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                logger.warn("Account locked for user: {}. Unlock time: {}", user.getEmail(), unlockTime);
                throw new UnauthorizedException("Account is temporarily locked due to multiple failed login attempts. Please try again after " + ACCOUNT_LOCK_DURATION_MINUTES + " minutes.");
            } else {
                // Unlock account
                logger.info("Unlocking account for user: {}", user.getEmail());
                user.resetFailedLoginAttempts();
                userRepository.save(user);
            }
        }
        
        // Check if email is verified
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Failed login attempt for user: {}. Current failed attempts: {}", user.getEmail(), user.getFailedLoginAttempts());
            
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            
            logger.info("Updated failed attempts for user: {} to {}", user.getEmail(), user.getFailedLoginAttempts());
            
            // Send account lock notification if account is now locked
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                logger.warn("Account locked for user: {} after {} failed attempts", user.getEmail(), user.getFailedLoginAttempts());
                try {
                    emailService.sendAccountLockNotification(user.getEmail(), user.getFirstName());
                    logger.info("Account lock notification sent to: {}", user.getEmail());
                } catch (Exception e) {
                    logger.error("Failed to send account lock notification to {}: {}", user.getEmail(), e.getMessage());
                }
            }
            
            throw new UnauthorizedException("Invalid email or password");
        }
        
        // Reset failed attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            logger.info("Resetting failed login attempts for user: {}", user.getEmail());
            user.resetFailedLoginAttempts();
            userRepository.save(user);
        }
        
        // Generate JWT token
        String token = jwtTokenService.generateToken(user, request.isRememberMe());
        long expiresIn = jwtTokenService.getTokenExpirationInSeconds(request.isRememberMe());
        
        // Create session records (handle Redis errors gracefully)
        try {
            createUserSession(user, token, clientInfo, request.isRememberMe());
        } catch (Exception e) {
            logger.warn("Failed to create session records for user {}: {}. Login will continue without session tracking.", user.getEmail(), e.getMessage());
        }
        
        logger.info("User authenticated successfully: {}", user.getEmail());
        
        return new LoginResponse(token, expiresIn, convertToUserDto(user));
    }
    
    @Override
    public void logoutUser(String token) {
        logger.info("Logging out user with token");
        
        try {
            String tokenHash = hashToken(token);
            
            // Deactivate database session
            userSessionRepository.deactivateSessionByTokenHash(tokenHash);
            
            // Remove Redis session (optional - don't fail if Redis is unavailable)
            try {
                Optional<RedisUserSession> redisSession = redisUserSessionRepository.findByTokenHash(tokenHash);
                redisSession.ifPresent(redisUserSessionRepository::delete);
                logger.debug("Redis session removed successfully");
            } catch (Exception e) {
                logger.warn("Failed to remove Redis session: {}. Database session was deactivated.", e.getMessage());
            }
            
            logger.info("User logged out successfully");
            
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed");
        }
    }
    
    @Override
    @Override
    public void initiatePasswordReset(String email, String clientInfo) {
        logger.info("Initiating password reset for email: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - but still log for debugging
            logger.info("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOpt.get();
        
        // Check rate limiting - allow 3 requests per hour per email
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = passwordResetTokenRepository.countRecentRequestsByUserId(user.getId(), oneHourAgo);
        
        logger.debug("Recent password reset requests for user {}: {} (limit: {})", user.getEmail(), recentRequests, MAX_RESET_REQUESTS_PER_HOUR);
        
        if (recentRequests >= MAX_RESET_REQUESTS_PER_HOUR) {
            logger.warn("Rate limit exceeded for password reset requests for user: {}", user.getEmail());
            throw new ValidationException("Too many password reset requests. Please try again later.");
        }
        
        // Invalidate existing tokens for this user
        passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
        logger.debug("Invalidated existing password reset tokens for user: {}", user.getEmail());
        
        // Create new reset token
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES);
        
        PasswordResetToken token = new PasswordResetToken(
            user.getId(),
            resetToken,
            expiresAt,
            extractIpFromClientInfo(clientInfo),
            extractUserAgentFromClientInfo(clientInfo)
        );
        
        passwordResetTokenRepository.save(token);
        logger.info("Created password reset token for user: {} (expires at: {})", user.getEmail(), expiresAt);
        
        // Send reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
            logger.info("Password reset email sent successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset email for user {}: {}", user.getEmail(), e.getMessage());
            // Don't throw exception - token is still valid
        }
    }
    
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        logger.info("Attempting to reset password with token: {}", request.getToken());
        
        if (!request.isPasswordMatching()) {
            logger.warn("Password reset failed: passwords do not match for token: {}", request.getToken());
            throw new ValidationException("Passwords do not match");
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getNewPassword());
        
        // Find valid token
        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .findValidTokenByToken(request.getToken(), now);
        
        if (tokenOpt.isEmpty()) {
            logger.warn("Password reset failed: invalid or expired token: {}", request.getToken());
            
            // Check if token exists but is expired or used
            Optional<PasswordResetToken> anyTokenOpt = passwordResetTokenRepository.findByToken(request.getToken());
            if (anyTokenOpt.isPresent()) {
                PasswordResetToken existingToken = anyTokenOpt.get();
                if (existingToken.isUsed()) {
                    logger.warn("Token already used: {}", request.getToken());
                } else if (existingToken.isExpired()) {
                    logger.warn("Token expired: {} (expired at: {})", request.getToken(), existingToken.getExpiresAt());
                }
            } else {
                logger.warn("Token not found: {}", request.getToken());
            }
            
            throw new ValidationException("Invalid or expired reset token");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
        
        if (userOpt.isEmpty()) {
            logger.error("User not found for valid reset token: {} (userId: {})", request.getToken(), resetToken.getUserId());
            throw new ValidationException("User not found");
        }
        
        User user = userOpt.get();
        logger.info("Resetting password for user: {}", user.getEmail());
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);
        logger.debug("Marked reset token as used: {}", request.getToken());
        
        // Invalidate all user sessions (logout from all devices)
        userSessionRepository.deactivateAllUserSessions(user.getId());
        logger.debug("Deactivated all sessions for user: {}", user.getEmail());
        
        // Remove Redis sessions (optional - don't fail if Redis is unavailable)
        try {
            redisUserSessionRepository.deleteByUserId(user.getId());
            logger.debug("Redis sessions deleted successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to delete Redis sessions for user {}: {}. Continuing with database session deactivation only.", user.getEmail(), e.getMessage());
        }
        
        // Send confirmation email
        try {
            emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getFirstName());
            logger.info("Password change confirmation email sent for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password change confirmation email for user {}: {}", user.getEmail(), e.getMessage());
            // Don't throw exception - password was already changed successfully
        }
        
        logger.info("Password reset completed successfully for user: {}", user.getEmail());
    }
    
    @Override
    public UserDto getUserProfile(String token) {
        User user = getUserFromToken(token);
        return convertToUserDto(user);
    }
    
    /**
     * Clean up expired password reset tokens
     * This method should be called periodically (e.g., via scheduled task)
     */
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository.deleteExpiredTokens(now);
        logger.debug("Cleaned up expired password reset tokens");
    }
    
    /**
     * Get password reset token info for debugging (admin only)
     */
    public String getPasswordResetTokenInfo(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "Token not found";
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        return String.format("Token: %s, Used: %s, Expired: %s, Created: %s, Expires: %s", 
            token, resetToken.isUsed(), resetToken.isExpired(), 
            resetToken.getCreatedAt(), resetToken.getExpiresAt());
    }
    
    @Override
    public UserDto validateTokenAndGetUser(String token) {
        User user = getUserFromToken(token);
        return convertToUserDto(user);
    }
    
    @Override
    public void resendEmailVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new ValidationException("User not found");
        }
        
        User user = userOpt.get();
        if (user.isEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }
        
        // Invalidate existing tokens
        emailVerificationTokenRepository.invalidateAllUserTokens(user.getId(), LocalDateTime.now());
        
        // Generate and send new verification token
        String verificationToken = generateVerificationToken(user);
        emailService.sendEmailVerification(user.getEmail(), user.getFirstName(), verificationToken);
        
        logger.info("Resent verification email to: {}", user.getEmail());
    }
    
    // Helper methods
    
    private User getUserFromToken(String token) {
        if (!jwtTokenService.validateTokenStructure(token)) {
            throw new UnauthorizedException("Invalid token");
        }
        
        UUID userId = jwtTokenService.extractUserId(token);
        if (userId == null) {
            throw new UnauthorizedException("Invalid token");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("User not found");
        }
        
        User user = userOpt.get();
        if (!jwtTokenService.validateToken(token, user)) {
            throw new UnauthorizedException("Token validation failed");
        }
        
        return user;
    }
    
    private void createUserSession(User user, String token, String clientInfo, boolean rememberMe) {
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(
            jwtTokenService.getTokenExpirationInSeconds(rememberMe)
        );
        
        // Create database session (this is essential and should not fail)
        try {
            UserSession session = new UserSession(
                user.getId(),
                tokenHash,
                expiresAt,
                clientInfo,
                extractIpFromClientInfo(clientInfo)
            );
            userSessionRepository.save(session);
            logger.debug("Database session created successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to create database session for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to create user session", e);
        }
        
        // Create Redis session (optional - don't fail if Redis is unavailable)
        try {
            RedisUserSession redisSession = new RedisUserSession(
                UUID.randomUUID().toString(), // Generate a unique ID for Redis session
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                tokenHash,
                clientInfo,
                extractIpFromClientInfo(clientInfo),
                jwtTokenService.getTokenExpirationInSeconds(rememberMe)
            );
            redisUserSessionRepository.save(redisSession);
            logger.debug("Redis session created successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to create Redis session for user {}: {}. Continuing with database session only.", user.getEmail(), e.getMessage());
        }
    }
    
    private UserDto convertToUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getDateOfBirth(),
            user.isEmailVerified(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    private String generateVerificationToken(User user) {
        // Generate unique verification token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(EMAIL_VERIFICATION_EXPIRY_HOURS);
        
        // Save token to database
        EmailVerificationToken verificationToken = new EmailVerificationToken(
            user.getId(),
            token,
            expiresAt
        );
        emailVerificationTokenRepository.save(verificationToken);
        
        return token;
    }
    
    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 12) {
            throw new ValidationException("Password must be at least 12 characters long");
        }
        
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> 
            "@$!%*?&".indexOf(ch) >= 0
        );
        
        if (!hasUpperCase) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }
        if (!hasLowerCase) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            throw new ValidationException("Password must contain at least one number");
        }
        if (!hasSpecialChar) {
            throw new ValidationException("Password must contain at least one special character (@$!%*?&)");
        }
    }
    
    private String hashToken(String token) {
        return String.valueOf(token.hashCode());
    }
    
    private String extractIpFromClientInfo(String clientInfo) {
        if (clientInfo != null && clientInfo.startsWith("IP: ")) {
            int commaIndex = clientInfo.indexOf(",");
            if (commaIndex > 0) {
                return clientInfo.substring(4, commaIndex);
            }
        }
        return "unknown";
    }
    
    private String extractUserAgentFromClientInfo(String clientInfo) {
        if (clientInfo != null && clientInfo.contains("User-Agent: ")) {
            int userAgentIndex = clientInfo.indexOf("User-Agent: ");
            if (userAgentIndex >= 0) {
                return clientInfo.substring(userAgentIndex + 12);
            }
        }
        return "unknown";
    }
}