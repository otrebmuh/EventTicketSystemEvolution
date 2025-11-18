package com.eventbooking.notification.messaging;

import com.eventbooking.common.messaging.MessageConsumer;
import com.eventbooking.common.messaging.TicketEvent;
import com.eventbooking.notification.service.TicketDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TicketEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketEventConsumer.class);
    
    private final MessageConsumer messageConsumer;
    private final TicketDeliveryService ticketDeliveryService;
    
    @Value("${aws.sqs.ticket-events-queue}")
    private String ticketEventsQueue;
    
    @Value("${aws.sqs.ticket-events-dlq}")
    private String ticketEventsDlq;
    
    public TicketEventConsumer(MessageConsumer messageConsumer,
                              TicketDeliveryService ticketDeliveryService) {
        this.messageConsumer = messageConsumer;
        this.ticketDeliveryService = ticketDeliveryService;
    }
    
    /**
     * Poll ticket events queue every 10 seconds
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void consumeTicketEvents() {
        try {
            messageConsumer.pollMessages(ticketEventsQueue, TicketEvent.class, this::handleTicketEvent);
        } catch (Exception e) {
            logger.error("Error polling ticket events queue: {}", e.getMessage(), e);
        }
    }
    
    private void handleTicketEvent(TicketEvent event) {
        logger.info("Processing ticket event: {} for order: {}", 
                event.getEventType(), event.getOrderId());
        
        try {
            switch (event.getEventType()) {
                case TICKETS_GENERATED:
                    handleTicketsGenerated(event);
                    break;
                case TICKET_CANCELLED:
                    handleTicketCancelled(event);
                    break;
                case TICKETS_DELIVERED:
                    handleTicketsDelivered(event);
                    break;
                case TICKET_DELIVERY_FAILED:
                    handleTicketDeliveryFailed(event);
                    break;
                default:
                    logger.warn("Unknown ticket event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to handle ticket event: {} for order: {}", 
                    event.getEventType(), event.getOrderId(), e);
            throw e; // Re-throw to trigger retry or DLQ
        }
    }
    
    private void handleTicketsGenerated(TicketEvent event) {
        logger.info("Handling tickets generated for order: {} with {} tickets", 
                event.getOrderId(), event.getQuantity());
        
        // Trigger ticket delivery
        // This would call ticketDeliveryService to send tickets via email
        // Implementation depends on having user email and ticket details
    }
    
    private void handleTicketCancelled(TicketEvent event) {
        logger.info("Handling ticket cancelled for order: {}", event.getOrderId());
        // Send ticket cancellation notification
    }
    
    private void handleTicketsDelivered(TicketEvent event) {
        logger.info("Handling tickets delivered for order: {}", event.getOrderId());
        // Log successful delivery
    }
    
    private void handleTicketDeliveryFailed(TicketEvent event) {
        logger.error("Handling ticket delivery failed for order: {} - {}", 
                event.getOrderId(), event.getErrorMessage());
        // Retry delivery or alert administrators
    }
}
