package com.eventbooking.common.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SagaEventStore for event sourcing
 * In production, this would be backed by a persistent store like PostgreSQL or DynamoDB
 */
public class InMemorySagaEventStore implements SagaEventStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemorySagaEventStore.class);
    
    private final Map<UUID, List<SagaEvent>> eventStore = new ConcurrentHashMap<>();
    
    @Override
    public void recordEvent(UUID sagaId, String eventType, String eventData, Map<String, Object> contextData) {
        SagaEvent event = new SagaEvent(sagaId, eventType, eventData, contextData);
        
        eventStore.computeIfAbsent(sagaId, k -> new ArrayList<>()).add(event);
        
        logger.debug("Recorded saga event: {} for saga: {} (Event ID: {})", 
                eventType, sagaId, event.getEventId());
    }
    
    @Override
    public List<SagaEvent> getEvents(UUID sagaId) {
        return new ArrayList<>(eventStore.getOrDefault(sagaId, Collections.emptyList()));
    }
    
    @Override
    public SagaExecutionSummary getSagaSummary(UUID sagaId) {
        List<SagaEvent> events = getEvents(sagaId);
        
        if (events.isEmpty()) {
            return null;
        }
        
        SagaExecutionSummary summary = new SagaExecutionSummary();
        summary.setSagaId(sagaId);
        summary.setTotalEvents(events.size());
        
        // Extract saga type from first event
        events.stream()
                .filter(e -> "SAGA_STARTED".equals(e.getEventType()))
                .findFirst()
                .ifPresent(e -> summary.setSagaType(e.getEventData()));
        
        // Get start time
        events.stream()
                .min(Comparator.comparing(SagaEvent::getTimestamp))
                .ifPresent(e -> summary.setStartTime(e.getTimestamp()));
        
        // Get end time
        events.stream()
                .max(Comparator.comparing(SagaEvent::getTimestamp))
                .ifPresent(e -> summary.setEndTime(e.getTimestamp()));
        
        // Calculate duration
        if (summary.getStartTime() != null && summary.getEndTime() != null) {
            Duration duration = Duration.between(summary.getStartTime(), summary.getEndTime());
            summary.setDurationMs(duration.toMillis());
        }
        
        // Determine status
        boolean completed = events.stream().anyMatch(e -> "SAGA_COMPLETED".equals(e.getEventType()));
        boolean failed = events.stream().anyMatch(e -> "SAGA_FAILED".equals(e.getEventType()));
        boolean compensated = events.stream().anyMatch(e -> "COMPENSATION_COMPLETED".equals(e.getEventType()));
        
        if (completed) {
            summary.setStatus("COMPLETED");
        } else if (compensated) {
            summary.setStatus("COMPENSATED");
            summary.setCompensated(true);
        } else if (failed) {
            summary.setStatus("FAILED");
        } else {
            summary.setStatus("IN_PROGRESS");
        }
        
        // Get completed steps
        List<String> completedSteps = events.stream()
                .filter(e -> "STEP_COMPLETED".equals(e.getEventType()))
                .map(SagaEvent::getEventData)
                .collect(Collectors.toList());
        summary.setCompletedSteps(completedSteps);
        
        // Get failed step
        events.stream()
                .filter(e -> "STEP_FAILED".equals(e.getEventType()))
                .findFirst()
                .ifPresent(e -> summary.setFailedStep(e.getEventData()));
        
        // Get error message from context
        events.stream()
                .filter(e -> "SAGA_FAILED".equals(e.getEventType()))
                .findFirst()
                .ifPresent(e -> {
                    if (e.getContextData() != null && e.getContextData().containsKey("errorMessage")) {
                        summary.setErrorMessage((String) e.getContextData().get("errorMessage"));
                    }
                });
        
        return summary;
    }
    
    /**
     * Clear all events (for testing purposes)
     */
    public void clear() {
        eventStore.clear();
        logger.info("Cleared all saga events from store");
    }
    
    /**
     * Get total number of sagas tracked
     */
    public int getSagaCount() {
        return eventStore.size();
    }
}
