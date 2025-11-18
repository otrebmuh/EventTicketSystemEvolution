package com.eventbooking.common.saga;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object that carries state throughout saga execution
 */
public class SagaContext {
    
    private final UUID sagaId;
    private final String sagaType;
    private final Map<String, Object> data;
    private final Instant startTime;
    private SagaStatus status;
    private String errorMessage;
    private String currentStep;
    
    public SagaContext(String sagaType) {
        this.sagaId = UUID.randomUUID();
        this.sagaType = sagaType;
        this.data = new HashMap<>();
        this.startTime = Instant.now();
        this.status = SagaStatus.STARTED;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
    
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
    
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    public UUID getSagaId() {
        return sagaId;
    }
    
    public String getSagaType() {
        return sagaType;
    }
    
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public SagaStatus getStatus() {
        return status;
    }
    
    public void setStatus(SagaStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
    
    public enum SagaStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        COMPENSATING,
        COMPENSATED,
        FAILED
    }
}
