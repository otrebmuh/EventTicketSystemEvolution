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
                throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
            } else {
                // Unlock account
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
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                emailService.sendAccountLockNotification(user.getEmail(), user.getFirstName());
            }
            
            throw new UnauthorizedException("Invalid email or password");
        }
        
        // Reset failed attempts on successful login
        user.resetFailedLoginAttempts();
        userRepository.save(user);
        
        // Generate JWT token
        String token = jwtTokenService.generateToken(user, request.isRememberMe());
        long expiresIn = jwtTokenService.getTokenExpirationInSeconds(request.isRememberMe());
        
        // Create session records
        createUserSession(user, token, clientInfo, request.isRememberMe());
        
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
    public void initiatePasswordReset(String email, String clientInfo) {
        logger.info("Initiating password reset for email: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists
            logger.info("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOpt.get();
        
        // Check rate limiting
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = passwordResetTokenRepository.countRecentRequestsByUserId(user.getId(), oneHourAgo);
        
        if (recentRequests >= MAX_RESET_REQUESTS_PER_HOUR) {
            throw new ValidationException("Too many password reset requests. Please try again later.");
        }
        
        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
        
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
        
        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
        
        logger.info("Password reset email sent for user: {}", user.getEmail());
    }
    
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        logger.info("Resetting password with token: {}", request.getToken());
        
        if (!request.isPasswordMatching()) {
            throw new ValidationException("Passwords do not match");
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getNewPassword());
        
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .findValidTokenByToken(request.getToken(), LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Invalid or expired reset token");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
        
        if (userOpt.isEmpty()) {
            throw new ValidationException("User not found");
        }
        
        User user = userOpt.get();
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);
        
        // Invalidate all user sessions (logout from all devices)
        userSessionRepository.deactivateAllUserSessions(user.getId());
        
        // Remove Redis sessions (optional - don't fail if Redis is unavailable)
        try {
            redisUserSessionRepository.deleteByUserId(user.getId());
            logger.debug("Redis sessions deleted successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to delete Redis sessions for user {}: {}. Continuing with database session deactivation only.", user.getEmail(), e.getMessage());
        }
        
        // Send confirmation email
        emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getFirstName());
        
        logger.info("Password reset successfully for user: {}", user.getEmail());
    }
    
    @Override
    public UserDto getUserProfile(String token) {
        User user = getUserFromToken(token);
        return convertToUserDto(user);
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
        
        // Create database session
        UserSession session = new UserSession(
            user.getId(),
            tokenHash,
            expiresAt,
            clientInfo,
            extractIpFromClientInfo(clientInfo)
        );
        userSessionRepository.save(session);
        
        // Create Redis session (optional - don't fail if Redis is unavailable)
        try {
            RedisUserSession redisSession = new RedisUserSession(
                session.getId().toString(),
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