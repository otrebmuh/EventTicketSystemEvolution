package com.eventbooking.payment.saga;

import com.eventbooking.common.saga.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Saga orchestrator for ticket purchase flow
 * Coordinates: Order Creation -> Payment Processing -> Ticket Generation -> Notification
 */
@Component
public class TicketPurchaseSaga {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketPurchaseSaga.class);
    
    private final ValidateInventoryStep validateInventoryStep;
    private final CreateOrderStep createOrderStep;
    private final ProcessPaymentStep processPaymentStep;
    private final ConfirmOrderStep confirmOrderStep;
    private final SagaEventStore eventStore;
    
    public TicketPurchaseSaga(
            ValidateInventoryStep validateInventoryStep,
            CreateOrderStep createOrderStep,
            ProcessPaymentStep processPaymentStep,
            ConfirmOrderStep confirmOrderStep,
            SagaEventStore eventStore) {
        this.validateInventoryStep = validateInventoryStep;
        this.createOrderStep = createOrderStep;
        this.processPaymentStep = processPaymentStep;
        this.confirmOrderStep = confirmOrderStep;
        this.eventStore = eventStore;
    }
    
    /**
     * Execute the ticket purchase saga
     * @param context The saga context with purchase details
     * @return true if purchase completed successfully, false otherwise
     */
    public boolean executePurchase(SagaContext context) {
        logger.info("Starting ticket purchase saga (Saga ID: {})", context.getSagaId());
        
        List<SagaStep> steps = Arrays.asList(
                validateInventoryStep,
                createOrderStep,
                processPaymentStep,
                confirmOrderStep
        );
        
        SagaOrchestrator orchestrator = new SagaOrchestrator(steps, eventStore);
        boolean success = orchestrator.execute(context);
        
        if (success) {
            logger.info("Ticket purchase saga completed successfully (Saga ID: {})", context.getSagaId());
        } else {
            logger.error("Ticket purchase saga failed (Saga ID: {})", context.getSagaId());
        }
        
        return success;
    }
    
    /**
     * Create a saga context for ticket purchase
     */
    public static SagaContext createPurchaseContext() {
        return new SagaContext("TICKET_PURCHASE");
    }
}
