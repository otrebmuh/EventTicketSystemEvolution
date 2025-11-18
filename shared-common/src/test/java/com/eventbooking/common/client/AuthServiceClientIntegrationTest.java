package com.eventbooking.common.client;

import com.eventbooking.common.dto.TokenValidationRequest;
import com.eventbooking.common.dto.TokenValidationResponse;
import com.eventbooking.common.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Integration tests for AuthServiceClient testing synchronous API calls
 */
class AuthServiceClientIntegrationTest {

    private AuthServiceClient authServiceClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private String authServiceUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

        authServiceClient = new AuthServiceClient(restTemplate, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void validateToken_Success() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();
        
        TokenValidationResponse expectedResponse = new TokenValidationResponse();
        expectedResponse.setValid(true);
        expectedResponse.setUserId(userId);
        expectedResponse.setEmail("user@example.com");

        mockServer.expect(requestTo(authServiceUrl + "/api/auth/internal/validate-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), 
                        MediaType.APPLICATION_JSON));

        // Act
        TokenValidationResponse response = authServiceClient.validateToken(token);

        // Assert
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(userId, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        mockServer.verify();
    }

    @Test
    void validateToken_InvalidToken() throws Exception {
        // Arrange
        String token = "invalid.jwt.token";
        
        TokenValidationResponse expectedResponse = new TokenValidationResponse();
        expectedResponse.setValid(false);

        mockServer.expect(requestTo(authServiceUrl + "/api/auth/internal/validate-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), 
                        MediaType.APPLICATION_JSON));

        // Act
        TokenValidationResponse response = authServiceClient.validateToken(token);

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        mockServer.verify();
    }

    @Test
    void getUserById_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserDto expectedUser = new UserDto();
        expectedUser.setId(userId);
        expectedUser.setEmail("user@example.com");
        expectedUser.setFirstName("John");
        expectedUser.setLastName("Doe");

        mockServer.expect(requestTo(authServiceUrl + "/api/auth/internal/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedUser), 
                        MediaType.APPLICATION_JSON));

        // Act
        UserDto user = authServiceClient.getUserById(userId);

        // Assert
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        mockServer.verify();
    }

    @Test
    void getUserById_NotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo(authServiceUrl + "/api/auth/internal/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(Exception.class, () -> authServiceClient.getUserById(userId));
        mockServer.verify();
    }

    @Test
    void getUsersByIds_Success() throws Exception {
        // Arrange
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIds = Arrays.asList(userId1, userId2);
        
        UserDto user1 = new UserDto();
        user1.setId(userId1);
        user1.setEmail("user1@example.com");
        
        UserDto user2 = new UserDto();
        user2.setId(userId2);
        user2.setEmail("user2@example.com");
        
        List<UserDto> expectedUsers = Arrays.asList(user1, user2);

        mockServer.expect(requestTo(authServiceUrl + "/api/auth/internal/users/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedUsers), 
                        MediaType.APPLICATION_JSON));

        // Act
        List<UserDto> users = authServiceClient.getUsersByIds(userIds);

        // Assert
        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals(userId1, users.get(0).getId());
        assertEquals(userId2, users.get(1).getId());
        mockServer.verify();
    }
}
