package com.eventbooking.common.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates saga execution with compensation logic
 */
public class SagaOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);
    
    private final List<SagaStep> steps;
    private final SagaEventStore eventStore;
    
    public SagaOrchestrator(List<SagaStep> steps, SagaEventStore eventStore) {
        this.steps = new ArrayList<>(steps);
        this.steps.sort(Comparator.comparingInt(SagaStep::getOrder));
        this.eventStore = eventStore;
    }
    
    /**
     * Execute the saga with all steps
     * @param context The saga context
     * @return true if saga completed successfully, false otherwise
     */
    public boolean execute(SagaContext context) {
        logger.info("Starting saga execution: {} (ID: {})", context.getSagaType(), context.getSagaId());
        
        // Record saga start
        eventStore.recordEvent(context.getSagaId(), "SAGA_STARTED", context.getSagaType(), null);
        
        List<SagaStep> executedSteps = new ArrayList<>();
        context.setStatus(SagaContext.SagaStatus.IN_PROGRESS);
        
        try {
            // Execute each step in order
            for (SagaStep step : steps) {
                context.setCurrentStep(step.getStepName());
                logger.info("Executing saga step: {} (Saga ID: {})", step.getStepName(), context.getSagaId());
                
                // Record step start
                eventStore.recordEvent(context.getSagaId(), "STEP_STARTED", step.getStepName(), context.getData());
                
                boolean success = step.execute(context);
                
                if (success) {
                    executedSteps.add(step);
                    logger.info("Saga step completed successfully: {} (Saga ID: {})", 
                            step.getStepName(), context.getSagaId());
                    
                    // Record step completion
                    eventStore.recordEvent(context.getSagaId(), "STEP_COMPLETED", step.getStepName(), context.getData());
                } else {
                    logger.error("Saga step failed: {} (Saga ID: {})", step.getStepName(), context.getSagaId());
                    
                    // Record step failure
                    eventStore.recordEvent(context.getSagaId(), "STEP_FAILED", step.getStepName(), 
                            context.getData());
                    
                    // Compensate all executed steps
                    compensate(context, executedSteps);
                    return false;
                }
            }
            
            // All steps completed successfully
            context.setStatus(SagaContext.SagaStatus.COMPLETED);
            Duration duration = Duration.between(context.getStartTime(), Instant.now());
            logger.info("Saga completed successfully: {} (ID: {}, Duration: {}ms)", 
                    context.getSagaType(), context.getSagaId(), duration.toMillis());
            
            // Record saga completion
            eventStore.recordEvent(context.getSagaId(), "SAGA_COMPLETED", context.getSagaType(), context.getData());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Saga execution failed with exception: {} (ID: {})", 
                    context.getSagaType(), context.getSagaId(), e);
            context.setErrorMessage(e.getMessage());
            
            // Record saga failure
            eventStore.recordEvent(context.getSagaId(), "SAGA_FAILED", context.getSagaType(), 
                    context.getData());
            
            // Compensate all executed steps
            compensate(context, executedSteps);
            return false;
        }
    }
    
    /**
     * Compensate (rollback) all executed steps in reverse order
     * @param context The saga context
     * @param executedSteps List of steps that were successfully executed
     */
    private void compensate(SagaContext context, List<SagaStep> executedSteps) {
        logger.warn("Starting compensation for saga: {} (ID: {})", 
                context.getSagaType(), context.getSagaId());
        
        context.setStatus(SagaContext.SagaStatus.COMPENSATING);
        
        // Record compensation start
        eventStore.recordEvent(context.getSagaId(), "COMPENSATION_STARTED", context.getSagaType(), 
                context.getData());
        
        // Compensate in reverse order
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = executedSteps.get(i);
            
            try {
                logger.info("Compensating saga step: {} (Saga ID: {})", 
                        step.getStepName(), context.getSagaId());
                
                // Record compensation step start
                eventStore.recordEvent(context.getSagaId(), "COMPENSATION_STEP_STARTED", 
                        step.getStepName(), context.getData());
                
                step.compensate(context);
                
                logger.info("Compensation completed for step: {} (Saga ID: {})", 
                        step.getStepName(), context.getSagaId());
                
                // Record compensation step completion
                eventStore.recordEvent(context.getSagaId(), "COMPENSATION_STEP_COMPLETED", 
                        step.getStepName(), context.getData());
                
            } catch (Exception e) {
                logger.error("Compensation failed for step: {} (Saga ID: {})", 
                        step.getStepName(), context.getSagaId(), e);
                
                // Record compensation failure
                eventStore.recordEvent(context.getSagaId(), "COMPENSATION_STEP_FAILED", 
                        step.getStepName(), context.getData());
                
                // Continue with other compensations even if one fails
            }
        }
        
        context.setStatus(SagaContext.SagaStatus.COMPENSATED);
        logger.info("Compensation completed for saga: {} (ID: {})", 
                context.getSagaType(), context.getSagaId());
        
        // Record compensation completion
        eventStore.recordEvent(context.getSagaId(), "COMPENSATION_COMPLETED", context.getSagaType(), 
                context.getData());
    }
}
