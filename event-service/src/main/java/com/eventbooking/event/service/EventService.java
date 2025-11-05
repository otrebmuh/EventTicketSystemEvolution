package com.eventbooking.event.service;

import com.eventbooking.event.dto.CreateEventRequest;
import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.UpdateEventRequest;
import com.eventbooking.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EventService {
    
    EventDto createEvent(CreateEventRequest request, UUID organizerId);
    
    EventDto getEventById(UUID eventId);
    
    EventDto updateEvent(UUID eventId, UpdateEventRequest request, UUID organizerId);
    
    void deleteEvent(UUID eventId, UUID organizerId);
    
    Page<EventDto> getEventsByOrganizer(UUID organizerId, Pageable pageable);
    
    Page<EventDto> getPublishedEvents(Pageable pageable);
    
    Page<EventDto> getUpcomingEvents(Pageable pageable);
    
    EventDto publishEvent(UUID eventId, UUID organizerId);
    
    EventDto cancelEvent(UUID eventId, UUID organizerId);
    
    EventDto updateEventImage(UUID eventId, String imageUrl, UUID organizerId);
    
    // Internal API for other services
    EventDto getEventForTicketService(UUID eventId);
}