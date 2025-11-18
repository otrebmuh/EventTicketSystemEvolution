package com.eventbooking.payment.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.common.saga.SagaEvent;
import com.eventbooking.common.saga.SagaEventStore;
import com.eventbooking.common.saga.SagaExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for monitoring saga execution and debugging
 */
@RestController
@RequestMapping("/api/saga")
public class SagaMonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaMonitoringController.class);
    
    private final SagaEventStore eventStore;
    
    public SagaMonitoringController(SagaEventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    /**
     * Get saga execution summary
     */
    @GetMapping("/{sagaId}/summary")
    public ResponseEntity<ApiResponse<SagaExecutionSummary>> getSagaSummary(
            @PathVariable UUID sagaId) {
        logger.info("Fetching saga summary for: {}", sagaId);
        
        SagaExecutionSummary summary = eventStore.getSagaSummary(sagaId);
        
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * Get all events for a saga
     */
    @GetMapping("/{sagaId}/events")
    public ResponseEntity<ApiResponse<List<SagaEvent>>> getSagaEvents(
            @PathVariable UUID sagaId) {
        logger.info("Fetching saga events for: {}", sagaId);
        
        List<SagaEvent> events = eventStore.getEvents(sagaId);
        
        if (events.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
