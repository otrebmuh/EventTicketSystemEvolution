package com.eventbooking.common.saga;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single event in saga execution history
 */
public class SagaEvent {
    
    private UUID eventId;
    private UUID sagaId;
    private String eventType;
    private String eventData;
    private Map<String, Object> contextData;
    private Instant timestamp;
    
    public SagaEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
    }
    
    public SagaEvent(UUID sagaId, String eventType, String eventData, Map<String, Object> contextData) {
        this();
        this.sagaId = sagaId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.contextData = contextData;
    }
    
    // Getters and Setters
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public UUID getSagaId() {
        return sagaId;
    }
    
    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getEventData() {
        return eventData;
    }
    
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    
    public Map<String, Object> getContextData() {
        return contextData;
    }
    
    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
