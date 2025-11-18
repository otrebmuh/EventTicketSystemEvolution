package com.eventbooking.common.client;

import com.eventbooking.common.dto.TokenValidationRequest;
import com.eventbooking.common.dto.TokenValidationResponse;
import com.eventbooking.common.dto.UserDto;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Component
public class AuthServiceClient extends ResilientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    @Value("${services.auth.url:http://localhost:8080}")
    private String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate,
                            CircuitBreakerRegistry circuitBreakerRegistry,
                            RetryRegistry retryRegistry) {
        super(restTemplate, circuitBreakerRegistry, retryRegistry, "authService");
    }

    public TokenValidationResponse validateToken(String token) {
        return executeWithResilience(() -> {
            String url = authServiceUrl + "/api/auth/internal/validate-token";
            TokenValidationRequest request = new TokenValidationRequest(token);
            
            log.debug("Validating token with auth service");
            ResponseEntity<TokenValidationResponse> response = restTemplate.postForEntity(
                    url, request, TokenValidationResponse.class);
            
            return response.getBody();
        });
    }

    public UserDto getUserById(UUID userId) {
        return executeWithResilience(() -> {
            String url = authServiceUrl + "/api/auth/internal/users/" + userId;
            
            log.debug("Fetching user {} from auth service", userId);
            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);
            
            return response.getBody();
        });
    }

    public List<UserDto> getUsersByIds(List<UUID> userIds) {
        return executeWithResilience(() -> {
            String url = authServiceUrl + "/api/auth/internal/users/batch";
            
            log.debug("Fetching {} users from auth service", userIds.size());
            ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(userIds),
                    new ParameterizedTypeReference<List<UserDto>>() {}
            );
            
            return response.getBody();
        });
    }
}
