package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.common.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga step to validate ticket inventory availability
 */
@Component
public class ValidateInventoryStep implements SagaStep {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateInventoryStep.class);
    
    @Override
    public boolean execute(SagaContext context) {
        logger.info("Validating inventory for saga: {}", context.getSagaId());
        
        try {
            UUID eventId = context.get("eventId", UUID.class);
            UUID ticketTypeId = context.get("ticketTypeId", UUID.class);
            Integer quantity = context.get("quantity", Integer.class);
            
            if (eventId == null || ticketTypeId == null || quantity == null) {
                logger.error("Missing required parameters for inventory validation");
                context.setErrorMessage("Missing required parameters");
                return false;
            }
            
            // In a real implementation, this would call the Ticket Service
            // to validate inventory availability
            // For now, we'll simulate the validation
            logger.info("Inventory validated for event: {}, ticket type: {}, quantity: {}", 
                    eventId, ticketTypeId, quantity);
            
            context.put("inventoryValidated", true);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to validate inventory", e);
            context.setErrorMessage("Inventory validation failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void compensate(SagaContext context) {
        logger.info("Compensating inventory validation for saga: {}", context.getSagaId());
        // No compensation needed for validation step
        logger.info("No compensation required for inventory validation");
    }
    
    @Override
    public String getStepName() {
        return "ValidateInventory";
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
}
