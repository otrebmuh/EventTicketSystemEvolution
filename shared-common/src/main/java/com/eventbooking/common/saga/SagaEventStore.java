package com.eventbooking.common.saga;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for storing saga events for event sourcing
 */
public interface SagaEventStore {
    
    /**
     * Record a saga event
     * @param sagaId The saga identifier
     * @param eventType The type of event
     * @param eventData Additional event data
     * @param contextData The saga context data at the time of the event
     */
    void recordEvent(UUID sagaId, String eventType, String eventData, Map<String, Object> contextData);
    
    /**
     * Get all events for a saga
     * @param sagaId The saga identifier
     * @return List of saga events
     */
    List<SagaEvent> getEvents(UUID sagaId);
    
    /**
     * Get saga execution history
     * @param sagaId The saga identifier
     * @return Saga execution summary
     */
    SagaExecutionSummary getSagaSummary(UUID sagaId);
}
