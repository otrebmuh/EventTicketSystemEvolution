package com.eventbooking.auth.controller;

import com.eventbooking.auth.entity.User;
import com.eventbooking.auth.repository.UserRepository;
import com.eventbooking.auth.service.JwtTokenService;
import com.eventbooking.common.dto.TokenValidationRequest;
import com.eventbooking.common.dto.TokenValidationResponse;
import com.eventbooking.common.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/internal")
public class InternalAuthController {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthController.class);

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request) {
        try {
            String token = request.getToken();
            
            if (!jwtTokenService.validateTokenStructure(token)) {
                return ResponseEntity.ok(TokenValidationResponse.invalid("INVALID_TOKEN", "Token is invalid or expired"));
            }

            UUID userId = jwtTokenService.extractUserId(token);
            String email = jwtTokenService.extractUsername(token);
            Date expiration = jwtTokenService.extractExpiration(token);
            LocalDateTime expiresAt = expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // For now, all users have USER role
            List<String> roles = List.of("USER");

            TokenValidationResponse response = TokenValidationResponse.valid(userId, email, roles, expiresAt);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.ok(TokenValidationResponse.invalid("VALIDATION_ERROR", "Error validating token"));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(this::toUserDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/batch")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestBody List<UUID> userIds) {
        List<UserDto> users = userRepository.findAllById(userIds).stream()
                .map(this::toUserDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
