package com.eventbooking.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redis model for user sessions
 * Provides fast access to active user sessions for authentication and authorization
 */
@RedisHash("user_session")
public class RedisUserSession implements Serializable {
    
    @Id
    private String id;
    
    @Indexed
    private UUID userId;
    
    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    
    @Indexed
    private String tokenHash;
    
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    
    @TimeToLive
    private Long ttl; // Time to live in seconds
    
    // Default constructor
    public RedisUserSession() {
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    // Constructor
    public RedisUserSession(String sessionId, UUID userId, String email, String firstName, 
                           String lastName, boolean emailVerified, String tokenHash, 
                           String deviceInfo, String ipAddress, Long ttl) {
        this.id = sessionId;
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailVerified = emailVerified;
        this.tokenHash = tokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.ttl = ttl;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }
    
    /**
     * Update last accessed time for session activity tracking
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "RedisUserSession{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", tokenHash='" + tokenHash + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }
}