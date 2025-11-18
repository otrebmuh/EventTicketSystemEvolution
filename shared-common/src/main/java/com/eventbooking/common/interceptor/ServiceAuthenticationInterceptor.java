package com.eventbooking.common.interceptor;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

@Component
public class ServiceAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthenticationInterceptor.class);

    @Value("${service.auth.secret:defaultServiceSecret123456789012345678901234567890}")
    private String serviceSecret;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                       ClientHttpRequestExecution execution) throws IOException {
        // Only add service token for internal API calls
        String path = request.getURI().getPath();
        if (path != null && path.contains("/internal/")) {
            String serviceToken = generateServiceToken();
            request.getHeaders().add("X-Service-Token", serviceToken);
            request.getHeaders().add("X-Service-Name", serviceName);
            log.debug("Added service authentication headers for request to: {}", request.getURI());
        }

        return execution.execute(request, body);
    }

    private String generateServiceToken() {
        SecretKey key = Keys.hmacShaKeyFor(serviceSecret.getBytes());
        
        return Jwts.builder()
                .subject(serviceName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000)) // 1 minute
                .claim("type", "SERVICE")
                .signWith(key)
                .compact();
    }
}
