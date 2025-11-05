package com.eventbooking.event.service;

import com.eventbooking.event.dto.*;
import com.eventbooking.event.entity.Event;
import com.eventbooking.event.entity.EventCategory;
import com.eventbooking.event.entity.EventStatus;
import com.eventbooking.event.entity.Venue;
import com.eventbooking.event.exception.EventAccessDeniedException;
import com.eventbooking.event.exception.EventNotFoundException;
import com.eventbooking.event.exception.InvalidEventDataException;
import com.eventbooking.event.mapper.EventMapper;
import com.eventbooking.event.repository.EventCategoryRepository;
import com.eventbooking.event.repository.EventRepository;
import com.eventbooking.event.repository.VenueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    
    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final EventMapper eventMapper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventServiceImpl(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            VenueRepository venueRepository,
            EventMapper eventMapper,
            CacheService cacheService,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.eventMapper = eventMapper;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public EventDto createEvent(CreateEventRequest request, UUID organizerId) {
        // Validate category exists
        EventCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new InvalidEventDataException("Category not found"));
        
        // Create or find venue
        Venue venue = createOrFindVenue(request.getVenue());
        
        // Create event
        Event event = new Event();
        event.setOrganizerId(organizerId);
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setVenue(venue);
        event.setCategory(category);
        event.setMaxCapacity(request.getMaxCapacity());
        event.setStatus(EventStatus.DRAFT);
        
        // Convert tags to JSON
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                event.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                throw new InvalidEventDataException("Invalid tags format");
            }
        }
        
        Event savedEvent = eventRepository.save(event);
        
        // Cache the event
        cacheService.cacheEvent(savedEvent);
        
        return eventMapper.toDto(savedEvent);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(UUID eventId) {
        // Check cache first
        Optional<Event> cachedEvent = cacheService.getCachedEvent(eventId);
        if (cachedEvent.isPresent()) {
            return eventMapper.toDto(cachedEvent.get());
        }
        
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Cache the event
        cacheService.cacheEvent(event);
        
        return eventMapper.toDto(event);
    }
    
    @Override
    public EventDto updateEvent(UUID eventId, UpdateEventRequest request, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Check if user is the organizer
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Not authorized to update this event");
        }
        
        // Update fields if provided
        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getVenue() != null) {
            Venue venue = createOrFindVenue(request.getVenue());
            event.setVenue(venue);
        }
        if (request.getCategoryId() != null) {
            EventCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InvalidEventDataException("Category not found"));
            event.setCategory(category);
        }
        if (request.getMaxCapacity() != null) {
            event.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getTags() != null) {
            try {
                event.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                throw new InvalidEventDataException("Invalid tags format");
            }
        }
        
        Event savedEvent = eventRepository.save(event);
        
        // Update cache
        cacheService.cacheEvent(savedEvent);
        
        return eventMapper.toDto(savedEvent);
    }
    
    @Override
    public void deleteEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Check if user is the organizer
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Not authorized to delete this event");
        }
        
        // Only allow deletion of draft events
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new InvalidEventDataException("Only draft events can be deleted");
        }
        
        eventRepository.delete(event);
        
        // Remove from cache
        cacheService.evictEvent(eventId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EventDto> getEventsByOrganizer(UUID organizerId, Pageable pageable) {
        Page<Event> events = eventRepository.findByOrganizerId(organizerId, pageable);
        return events.map(eventMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EventDto> getPublishedEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findByStatus(EventStatus.PUBLISHED, pageable);
        return events.map(eventMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EventDto> getUpcomingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
        return events.map(eventMapper::toDto);
    }
    
    @Override
    public EventDto publishEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Check if user is the organizer
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Not authorized to publish this event");
        }
        
        // Validate event is ready for publishing
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new InvalidEventDataException("Only draft events can be published");
        }
        
        event.setStatus(EventStatus.PUBLISHED);
        Event savedEvent = eventRepository.save(event);
        
        // Update cache
        cacheService.cacheEvent(savedEvent);
        
        // TODO: Publish event to SNS for notifications
        
        return eventMapper.toDto(savedEvent);
    }
    
    @Override
    public EventDto cancelEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Check if user is the organizer
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Not authorized to cancel this event");
        }
        
        event.setStatus(EventStatus.CANCELLED);
        Event savedEvent = eventRepository.save(event);
        
        // Update cache
        cacheService.cacheEvent(savedEvent);
        
        // TODO: Publish cancellation event to SNS for notifications
        
        return eventMapper.toDto(savedEvent);
    }
    
    @Override
    public EventDto updateEventImage(UUID eventId, String imageUrl, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        // Check if user is the organizer
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new EventAccessDeniedException("Not authorized to update this event");
        }
        
        event.setImageUrl(imageUrl);
        Event savedEvent = eventRepository.save(event);
        
        // Update cache
        cacheService.cacheEvent(savedEvent);
        
        return eventMapper.toDto(savedEvent);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EventDto getEventForTicketService(UUID eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        return eventMapper.toDto(event);
    }
    
    private Venue createOrFindVenue(VenueRequest venueRequest) {
        // Try to find existing venue by name and city
        Optional<Venue> existingVenue = venueRepository
            .findByNameIgnoreCaseAndCityIgnoreCase(venueRequest.getName(), venueRequest.getCity());
        
        if (existingVenue.isPresent()) {
            return existingVenue.get();
        }
        
        // Create new venue
        Venue venue = new Venue();
        venue.setName(venueRequest.getName());
        venue.setAddress(venueRequest.getAddress());
        venue.setCity(venueRequest.getCity());
        venue.setState(venueRequest.getState());
        venue.setZipCode(venueRequest.getZipCode());
        venue.setCountry(venueRequest.getCountry());
        venue.setLatitude(venueRequest.getLatitude());
        venue.setLongitude(venueRequest.getLongitude());
        venue.setMaxCapacity(venueRequest.getMaxCapacity());
        venue.setVenueType(venueRequest.getVenueType());
        
        return venueRepository.save(venue);
    }
}