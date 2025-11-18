package com.eventbooking.common.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String email;
    private List<String> roles;
    private LocalDateTime expiresAt;
    private String error;
    private String message;

    public TokenValidationResponse() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static TokenValidationResponse valid(UUID userId, String email, List<String> roles, LocalDateTime expiresAt) {
        TokenValidationResponse response = new TokenValidationResponse();
        response.setValid(true);
        response.setUserId(userId);
        response.setEmail(email);
        response.setRoles(roles);
        response.setExpiresAt(expiresAt);
        return response;
    }

    public static TokenValidationResponse invalid(String error, String message) {
        TokenValidationResponse response = new TokenValidationResponse();
        response.setValid(false);
        response.setError(error);
        response.setMessage(message);
        return response;
    }
}
