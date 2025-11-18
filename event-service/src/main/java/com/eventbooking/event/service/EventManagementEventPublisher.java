package com.eventbooking.event.service;

import com.eventbooking.common.messaging.EventManagementEvent;
import com.eventbooking.common.messaging.MessagePublisher;
import com.eventbooking.event.entity.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class EventManagementEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventManagementEventPublisher.class);
    
    private final MessagePublisher messagePublisher;
    
    @Value("${aws.sns.event-management-topic}")
    private String eventManagementTopic;
    
    public EventManagementEventPublisher(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }
    
    public void publishEventCreated(Event event) {
        try {
            EventManagementEvent managementEvent = new EventManagementEvent(
                    EventManagementEvent.EventType.EVENT_CREATED,
                    event.getId(),
                    event.getOrganizerId()
            );
            managementEvent.setEventName(event.getName());
            managementEvent.setEventStatus(event.getStatus().toString());
            managementEvent.setEventDate(event.getEventDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
            
            messagePublisher.publishToTopic(eventManagementTopic, managementEvent);
            
            logger.info("Published EVENT_CREATED event for event: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to publish event created for event: {}", event.getId(), e);
        }
    }
    
    public void publishEventUpdated(Event event) {
        try {
            EventManagementEvent managementEvent = new EventManagementEvent(
                    EventManagementEvent.EventType.EVENT_UPDATED,
                    event.getId(),
                    event.getOrganizerId()
            );
            managementEvent.setEventName(event.getName());
            managementEvent.setEventStatus(event.getStatus().toString());
            managementEvent.setEventDate(event.getEventDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
            
            messagePublisher.publishToTopic(eventManagementTopic, managementEvent);
            
            logger.info("Published EVENT_UPDATED event for event: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to publish event updated for event: {}", event.getId(), e);
        }
    }
    
    public void publishEventCancelled(Event event) {
        try {
            EventManagementEvent managementEvent = new EventManagementEvent(
                    EventManagementEvent.EventType.EVENT_CANCELLED,
                    event.getId(),
                    event.getOrganizerId()
            );
            managementEvent.setEventName(event.getName());
            managementEvent.setEventStatus(event.getStatus().toString());
            managementEvent.setEventDate(event.getEventDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
            
            messagePublisher.publishToTopic(eventManagementTopic, managementEvent);
            
            logger.info("Published EVENT_CANCELLED event for event: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to publish event cancelled for event: {}", event.getId(), e);
        }
    }
    
    public void publishEventPublished(Event event) {
        try {
            EventManagementEvent managementEvent = new EventManagementEvent(
                    EventManagementEvent.EventType.EVENT_PUBLISHED,
                    event.getId(),
                    event.getOrganizerId()
            );
            managementEvent.setEventName(event.getName());
            managementEvent.setEventStatus(event.getStatus().toString());
            managementEvent.setEventDate(event.getEventDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
            
            messagePublisher.publishToTopic(eventManagementTopic, managementEvent);
            
            logger.info("Published EVENT_PUBLISHED event for event: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to publish event published for event: {}", event.getId(), e);
        }
    }
}
