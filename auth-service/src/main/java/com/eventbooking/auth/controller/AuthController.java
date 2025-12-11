package com.eventbooking.auth.controller;

import com.eventbooking.auth.dto.*;
import com.eventbooking.auth.service.AuthService;
import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.common.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * User registration endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        try {
            UserDto user = authService.registerUser(request, getClientInfo(httpRequest));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully. Please check your email for verification.", user));
        } catch (Exception e) {
            logger.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Email verification endpoint
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        
        logger.info("Email verification attempt with token: {}", request.getToken());
        
        try {
            authService.verifyEmail(request.getToken());
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now log in."));
        } catch (Exception e) {
            logger.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Resend email verification endpoint
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        
        logger.info("Resend verification request for email: {}", request.getEmail());
        
        try {
            authService.resendEmailVerification(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Verification email sent. Please check your email."));
        } catch (Exception e) {
            logger.error("Resend verification failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Login attempt for email: {}", request.getEmail());
        
        try {
            LoginResponse response = authService.authenticateUser(request, getClientInfo(httpRequest));
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            logger.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * User logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null) {
            try {
                authService.logoutUser(token);
                return ResponseEntity.ok(ApiResponse.success("Logout successful"));
            } catch (Exception e) {
                logger.error("Logout failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(e.getMessage()));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
    
    /**
     * Forgot password endpoint
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Password reset request for email: {}", request.getEmail());
        
        try {
            authService.initiatePasswordReset(request.getEmail(), getClientInfo(httpRequest));
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent. Please check your email."));
        } catch (Exception e) {
            logger.error("Password reset request failed for email {}: {}", request.getEmail(), e.getMessage());
            // Don't reveal if email exists or not for security
            return ResponseEntity.ok(ApiResponse.success("If the email exists, a password reset link has been sent."));
        }
    }
    
    /**
     * Reset password endpoint
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        
        logger.info("Password reset attempt with token: {}", request.getToken());
        
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now log in with your new password."));
        } catch (Exception e) {
            logger.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authorization token required"));
        }
        
        try {
            UserDto user = authService.getUserProfile(token);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            logger.error("Get profile failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Validate token endpoint (for other services)
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<UserDto>> validateToken(@RequestParam String token) {
        
        try {
            UserDto user = authService.validateTokenAndGetUser(token);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * Extract client information from request
     */
    private String getClientInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        return String.format("IP: %s, User-Agent: %s", ipAddress, userAgent);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}