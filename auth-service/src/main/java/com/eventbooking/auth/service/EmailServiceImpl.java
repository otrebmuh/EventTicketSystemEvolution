package com.eventbooking.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from:noreply@eventbooking.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void sendEmailVerification(String email, String firstName, String verificationToken) {
        logger.info("Sending email verification to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Verify Your Email - Event Booking System");
            
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for registering with Event Booking System!\n\n" +
                "Please click the link below to verify your email address:\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Event Booking System Team",
                firstName, verificationUrl
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Email verification sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.warn("Failed to send email verification to {}: {} (Email service may not be configured)", email, e.getMessage());
            // Don't throw exception - allow registration to continue even if email fails
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String email, String firstName, String resetToken) {
        logger.info("Sending password reset email to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Password Reset - Event Booking System");
            
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "You have requested to reset your password for Event Booking System.\n\n" +
                "Please click the link below to reset your password:\n" +
                "%s\n\n" +
                "This link will expire in 15 minutes for security reasons.\n\n" +
                "If you didn't request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Event Booking System Team",
                firstName, resetUrl
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Password reset email sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.warn("Failed to send password reset email to {}: {} (Email service may not be configured)", email, e.getMessage());
            // Don't throw exception for password reset emails in development
        }
    }
    
    @Override
    public void sendPasswordChangeConfirmation(String email, String firstName) {
        logger.info("Sending password change confirmation to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Password Changed - Event Booking System");
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your password has been successfully changed for Event Booking System.\n\n" +
                "You have been logged out from all devices for security reasons.\n\n" +
                "If you didn't make this change, please contact our support team immediately.\n\n" +
                "Best regards,\n" +
                "Event Booking System Team",
                firstName
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Password change confirmation sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.error("Failed to send password change confirmation to {}: {}", email, e.getMessage());
            // Don't throw exception for confirmation emails
        }
    }
    
    @Override
    public void sendAccountLockNotification(String email, String firstName) {
        logger.info("Sending account lock notification to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Account Temporarily Locked - Event Booking System");
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your account has been temporarily locked due to multiple failed login attempts.\n\n" +
                "For security reasons, your account will be automatically unlocked after 30 minutes.\n\n" +
                "If you believe this was not you, please contact our support team.\n\n" +
                "Best regards,\n" +
                "Event Booking System Team",
                firstName
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("Account lock notification sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.error("Failed to send account lock notification to {}: {}", email, e.getMessage());
            // Don't throw exception for notification emails
        }
    }
}