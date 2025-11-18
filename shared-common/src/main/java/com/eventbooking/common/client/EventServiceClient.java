package com.eventbooking.common.client;

import com.eventbooking.common.dto.EventDto;
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
import java.util.Map;
import java.util.UUID;

@Component
public class EventServiceClient extends ResilientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EventServiceClient.class);

    @Value("${services.event.url:http://localhost:8081}")
    private String eventServiceUrl;

    public EventServiceClient(RestTemplate restTemplate,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             RetryRegistry retryRegistry) {
        super(restTemplate, circuitBreakerRegistry, retryRegistry, "eventService");
    }

    public EventDto getEventById(UUID eventId) {
        return executeWithResilience(() -> {
            String url = eventServiceUrl + "/api/events/internal/" + eventId;
            
            log.debug("Fetching event {} from event service", eventId);
            ResponseEntity<EventDto> response = restTemplate.getForEntity(url, EventDto.class);
            
            return response.getBody();
        });
    }

    public boolean validateOrganizer(UUID eventId, UUID userId) {
        return executeWithResilienceAndFallback(() -> {
            String url = eventServiceUrl + "/api/events/internal/" + eventId + "/validate-organizer/" + userId;
            
            log.debug("Validating organizer for event {}", eventId);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("isOrganizer"));
        }, false);
    }

    public List<EventDto> getEventsByIds(List<UUID> eventIds) {
        return executeWithResilience(() -> {
            String url = eventServiceUrl + "/api/events/internal/batch";
            
            log.debug("Fetching {} events from event service", eventIds.size());
            ResponseEntity<List<EventDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(eventIds),
                    new ParameterizedTypeReference<List<EventDto>>() {}
            );
            
            return response.getBody();
        });
    }
}
