package com.eventbooking.common.saga;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Summary of saga execution for monitoring and debugging
 */
public class SagaExecutionSummary {
    
    private UUID sagaId;
    private String sagaType;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private Long durationMs;
    private List<String> completedSteps;
    private String failedStep;
    private String errorMessage;
    private boolean compensated;
    private int totalEvents;
    
    // Getters and Setters
    public UUID getSagaId() {
        return sagaId;
    }
    
    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }
    
    public String getSagaType() {
        return sagaType;
    }
    
    public void setSagaType(String sagaType) {
        this.sagaType = sagaType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public List<String> getCompletedSteps() {
        return completedSteps;
    }
    
    public void setCompletedSteps(List<String> completedSteps) {
        this.completedSteps = completedSteps;
    }
    
    public String getFailedStep() {
        return failedStep;
    }
    
    public void setFailedStep(String failedStep) {
        this.failedStep = failedStep;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isCompensated() {
        return compensated;
    }
    
    public void setCompensated(boolean compensated) {
        this.compensated = compensated;
    }
    
    public int getTotalEvents() {
        return totalEvents;
    }
    
    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }
}
