package com.eventbooking.common.client;

import com.eventbooking.common.dto.EventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Integration tests for EventServiceClient testing synchronous API calls
 */
class EventServiceClientIntegrationTest {

    private EventServiceClient eventServiceClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private String eventServiceUrl = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

        eventServiceClient = new EventServiceClient(restTemplate, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void getEventById_Success() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        
        EventDto expectedEvent = new EventDto();
        expectedEvent.setId(eventId);
        expectedEvent.setOrganizerId(organizerId);
        expectedEvent.setName("Test Event");
        expectedEvent.setDescription("Test Description");
        expectedEvent.setEventDate(LocalDateTime.now().plusDays(7));
        expectedEvent.setVenueName("Test Venue");
        expectedEvent.setVenueAddress("123 Test St");
        expectedEvent.setCategory("MUSIC");
        expectedEvent.setStatus("PUBLISHED");

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/" + eventId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedEvent), 
                        MediaType.APPLICATION_JSON));

        // Act
        EventDto event = eventServiceClient.getEventById(eventId);

        // Assert
        assertNotNull(event);
        assertEquals(eventId, event.getId());
        assertEquals("Test Event", event.getName());
        assertEquals(organizerId, event.getOrganizerId());
        mockServer.verify();
    }

    @Test
    void getEventById_NotFound() {
        // Arrange
        UUID eventId = UUID.randomUUID();

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/" + eventId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(Exception.class, () -> eventServiceClient.getEventById(eventId));
        mockServer.verify();
    }

    @Test
    void validateOrganizer_Success() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Map<String, Object> response = new HashMap<>();
        response.put("isOrganizer", true);

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/" + eventId + 
                "/validate-organizer/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), 
                        MediaType.APPLICATION_JSON));

        // Act
        boolean isOrganizer = eventServiceClient.validateOrganizer(eventId, userId);

        // Assert
        assertTrue(isOrganizer);
        mockServer.verify();
    }

    @Test
    void validateOrganizer_NotOrganizer() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Map<String, Object> response = new HashMap<>();
        response.put("isOrganizer", false);

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/" + eventId + 
                "/validate-organizer/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), 
                        MediaType.APPLICATION_JSON));

        // Act
        boolean isOrganizer = eventServiceClient.validateOrganizer(eventId, userId);

        // Assert
        assertFalse(isOrganizer);
        mockServer.verify();
    }

    @Test
    void validateOrganizer_ServiceFailure_ReturnsFallback() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/" + eventId + 
                "/validate-organizer/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        boolean isOrganizer = eventServiceClient.validateOrganizer(eventId, userId);

        // Assert - should return fallback value (false)
        assertFalse(isOrganizer);
        mockServer.verify();
    }

    @Test
    void getEventsByIds_Success() throws Exception {
        // Arrange
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        List<UUID> eventIds = Arrays.asList(eventId1, eventId2);
        
        EventDto event1 = new EventDto();
        event1.setId(eventId1);
        event1.setName("Event 1");
        
        EventDto event2 = new EventDto();
        event2.setId(eventId2);
        event2.setName("Event 2");
        
        List<EventDto> expectedEvents = Arrays.asList(event1, event2);

        mockServer.expect(requestTo(eventServiceUrl + "/api/events/internal/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedEvents), 
                        MediaType.APPLICATION_JSON));

        // Act
        List<EventDto> events = eventServiceClient.getEventsByIds(eventIds);

        // Assert
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals(eventId1, events.get(0).getId());
        assertEquals(eventId2, events.get(1).getId());
        mockServer.verify();
    }
}
