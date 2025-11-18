package com.eventbooking.ticket.service;

import com.eventbooking.common.messaging.MessagePublisher;
import com.eventbooking.common.messaging.TicketEvent;
import com.eventbooking.ticket.dto.TicketDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketEventPublisher.class);
    
    private final MessagePublisher messagePublisher;
    
    @Value("${aws.sqs.ticket-events-queue}")
    private String ticketEventsQueue;
    
    @Value("${aws.sns.ticket-events-topic}")
    private String ticketEventsTopic;
    
    public TicketEventPublisher(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }
    
    public void publishTicketsGenerated(UUID orderId, UUID userId, UUID eventId, 
                                       List<TicketDto> tickets, String holderName, String holderEmail) {
        try {
            TicketEvent event = new TicketEvent(
                    TicketEvent.EventType.TICKETS_GENERATED,
                    orderId,
                    userId
            );
            event.setEventId(eventId);
            event.setTicketIds(tickets.stream()
                    .map(TicketDto::getId)
                    .collect(Collectors.toList()));
            event.setQuantity(tickets.size());
            event.setHolderName(holderName);
            event.setHolderEmail(holderEmail);
            
            // Publish to both queue and topic
            messagePublisher.publishToQueue(ticketEventsQueue, event);
            messagePublisher.publishToTopic(ticketEventsTopic, event);
            
            logger.info("Published TICKETS_GENERATED event for order: {} with {} tickets", 
                    orderId, tickets.size());
        } catch (Exception e) {
            logger.error("Failed to publish tickets generated event for order: {}", 
                    orderId, e);
            // Don't throw exception - tickets already generated
        }
    }
    
    public void publishTicketCancelled(UUID ticketId, UUID orderId, UUID userId) {
        try {
            TicketEvent event = new TicketEvent(
                    TicketEvent.EventType.TICKET_CANCELLED,
                    orderId,
                    userId
            );
            event.setTicketIds(List.of(ticketId));
            
            messagePublisher.publishToQueue(ticketEventsQueue, event);
            messagePublisher.publishToTopic(ticketEventsTopic, event);
            
            logger.info("Published TICKET_CANCELLED event for ticket: {}", ticketId);
        } catch (Exception e) {
            logger.error("Failed to publish ticket cancelled event for ticket: {}", 
                    ticketId, e);
        }
    }
    
    public void publishTicketsDelivered(UUID orderId, UUID userId, List<UUID> ticketIds) {
        try {
            TicketEvent event = new TicketEvent(
                    TicketEvent.EventType.TICKETS_DELIVERED,
                    orderId,
                    userId
            );
            event.setTicketIds(ticketIds);
            event.setQuantity(ticketIds.size());
            
            messagePublisher.publishToQueue(ticketEventsQueue, event);
            messagePublisher.publishToTopic(ticketEventsTopic, event);
            
            logger.info("Published TICKETS_DELIVERED event for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to publish tickets delivered event for order: {}", 
                    orderId, e);
        }
    }
    
    public void publishTicketDeliveryFailed(UUID orderId, UUID userId, String errorMessage) {
        try {
            TicketEvent event = new TicketEvent(
                    TicketEvent.EventType.TICKET_DELIVERY_FAILED,
                    orderId,
                    userId
            );
            event.setErrorMessage(errorMessage);
            
            messagePublisher.publishToQueue(ticketEventsQueue, event);
            messagePublisher.publishToTopic(ticketEventsTopic, event);
            
            logger.info("Published TICKET_DELIVERY_FAILED event for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to publish ticket delivery failed event for order: {}", 
                    orderId, e);
        }
    }
}
