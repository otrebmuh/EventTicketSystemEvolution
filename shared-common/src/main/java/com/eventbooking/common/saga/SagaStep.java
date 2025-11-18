package com.eventbooking.common.saga;

import java.util.UUID;

/**
 * Represents a single step in a saga transaction
 */
public interface SagaStep {
    
    /**
     * Execute the forward transaction
     * @param context The saga execution context
     * @return true if successful, false otherwise
     */
    boolean execute(SagaContext context);
    
    /**
     * Execute the compensation (rollback) action
     * @param context The saga execution context
     */
    void compensate(SagaContext context);
    
    /**
     * Get the name of this saga step
     * @return step name
     */
    String getStepName();
    
    /**
     * Get the order/sequence of this step
     * @return step order
     */
    int getOrder();
}
