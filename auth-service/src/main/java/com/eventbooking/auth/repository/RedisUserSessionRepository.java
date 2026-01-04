package com.eventbooking.auth.repository;

import com.eventbooking.auth.model.RedisUserSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Redis-based user session management
 * Provides fast access to active user sessions for authentication and authorization
 */
@Repository
public interface RedisUserSessionRepository extends CrudRepository<RedisUserSession, String> {
    
    /**
     * Find all sessions for a specific user
     * Used for managing multiple device logins and session cleanup
     */
    List<RedisUserSession> findByUserId(UUID userId);
    
    /**
     * Find session by token hash
     * Used for fast token validation during API requests
     */
    Optional<RedisUserSession> findByTokenHash(String tokenHash);
    
    /**
     * Delete all sessions for a user
     * Used during password reset, account deactivation, or logout from all devices
     */
    void deleteByUserId(UUID userId);
    
    /**
     * Find sessions by email
     * Used for administrative purposes and user management
     */
    List<RedisUserSession> findByEmail(String email);
    
    /**
     * Count active sessions for a user
     * Used for session limit enforcement
     */
    long countByUserId(UUID userId);
}