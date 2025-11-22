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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCategoryRepository categoryRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private UUID organizerId;
    private UUID eventId;
    private UUID categoryId;
    private Event testEvent;
    private EventCategory testCategory;
    private Venue testVenue;
    private EventDto testEventDto;

    @BeforeEach
    void setUp() {
        organizerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        testCategory = new EventCategory();
        testCategory.setId(categoryId);
        testCategory.setName("Music");

        testVenue = new Venue();
        testVenue.setId(UUID.randomUUID());
        testVenue.setName("Test Arena");
        testVenue.setCity("New York");
        testVenue.setAddress("123 Test St");
        testVenue.setState("NY");
        testVenue.setZipCode("10001");
        testVenue.setCountry("USA");

        testEvent = new Event();
        testEvent.setId(eventId);
        testEvent.setOrganizerId(organizerId);
        testEvent.setName("Test Concert");
        testEvent.setDescription("A great concert");
        testEvent.setEventDate(LocalDateTime.now().plusDays(30));
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setStatus(EventStatus.DRAFT);
        testEvent.setMaxCapacity(1000);

        testEventDto = new EventDto();
        testEventDto.setId(eventId);
        testEventDto.setName("Test Concert");
    }

    // ========== Event Creation Tests ==========

    @Test
    void createEvent_WithValidData_ShouldCreateEvent() throws JsonProcessingException {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Test Concert");
        request.setDescription("A great concert");
        request.setEventDate(LocalDateTime.now().plusDays(30));
        request.setCategoryId(categoryId);
        request.setMaxCapacity(1000);
        request.setTags(Arrays.asList("music", "rock"));
        
        VenueRequest venueRequest = new VenueRequest();
        venueRequest.setName("Test Arena");
        venueRequest.setCity("New York");
        venueRequest.setAddress("123 Test St");
        venueRequest.setState("NY");
        venueRequest.setZipCode("10001");
        venueRequest.setCountry("USA");
        request.setVenue(venueRequest);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(venueRepository.findByNameIgnoreCaseAndCityIgnoreCase(anyString(), anyString()))
            .thenReturn(Optional.of(testVenue));
        when(objectMapper.writeValueAsString(anyList())).thenReturn("[\"music\",\"rock\"]");
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.createEvent(request, organizerId);

        assertNotNull(result);
        assertEquals(testEventDto.getName(), result.getName());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_WithInvalidCategory_ShouldThrowException() {
        CreateEventRequest request = new CreateEventRequest();
        request.setCategoryId(UUID.randomUUID());

        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(InvalidEventDataException.class, () -> 
            eventService.createEvent(request, organizerId)
        );
    }

    @Test
    void createEvent_WithNewVenue_ShouldCreateVenue() throws JsonProcessingException {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Test Concert");
        request.setEventDate(LocalDateTime.now().plusDays(30));
        request.setCategoryId(categoryId);
        request.setMaxCapacity(1000);
        
        VenueRequest venueRequest = new VenueRequest();
        venueRequest.setName("New Arena");
        venueRequest.setCity("Boston");
        venueRequest.setAddress("456 New St");
        venueRequest.setState("MA");
        venueRequest.setZipCode("02101");
        venueRequest.setCountry("USA");
        request.setVenue(venueRequest);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(venueRepository.findByNameIgnoreCaseAndCityIgnoreCase(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(venueRepository.save(any(Venue.class))).thenReturn(testVenue);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.createEvent(request, organizerId);

        assertNotNull(result);
        verify(venueRepository).save(any(Venue.class));
        verify(eventRepository).save(any(Event.class));
    }

    // ========== Event Retrieval Tests ==========

    @Test
    void getEventById_WithExistingEvent_ShouldReturnEvent() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.getEventById(eventId);

        assertNotNull(result);
        assertEquals(testEventDto.getName(), result.getName());
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithValidId_ShouldFetchFromDatabase() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.getEventById(eventId);

        assertNotNull(result);
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventById_WithNonExistentEvent_ShouldThrowException() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> 
            eventService.getEventById(eventId)
        );
    }

    // ========== Event Update Tests ==========

    @Test
    void updateEvent_WithValidData_ShouldUpdateEvent() {
        UpdateEventRequest request = new UpdateEventRequest();
        request.setName("Updated Concert");
        request.setDescription("Updated description");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.updateEvent(eventId, request, organizerId);

        assertNotNull(result);
        verify(eventRepository).save(testEvent);
    }

    @Test
    void updateEvent_WithUnauthorizedUser_ShouldThrowException() {
        UpdateEventRequest request = new UpdateEventRequest();
        UUID differentOrganizerId = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(EventAccessDeniedException.class, () -> 
            eventService.updateEvent(eventId, request, differentOrganizerId)
        );
    }

    @Test
    void updateEvent_WithNonExistentEvent_ShouldThrowException() {
        UpdateEventRequest request = new UpdateEventRequest();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> 
            eventService.updateEvent(eventId, request, organizerId)
        );
    }

    // ========== Event Deletion Tests ==========

    @Test
    void deleteEvent_WithDraftEvent_ShouldDeleteEvent() {
        testEvent.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        eventService.deleteEvent(eventId, organizerId);

        verify(eventRepository).delete(testEvent);
    }

    @Test
    void deleteEvent_WithPublishedEvent_ShouldThrowException() {
        testEvent.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(InvalidEventDataException.class, () -> 
            eventService.deleteEvent(eventId, organizerId)
        );
    }

    @Test
    void deleteEvent_WithUnauthorizedUser_ShouldThrowException() {
        UUID differentOrganizerId = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(EventAccessDeniedException.class, () -> 
            eventService.deleteEvent(eventId, differentOrganizerId)
        );
    }

    // ========== Event Status Management Tests ==========

    @Test
    void publishEvent_WithDraftEvent_ShouldPublishEvent() {
        testEvent.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.publishEvent(eventId, organizerId);

        assertNotNull(result);
        assertEquals(EventStatus.PUBLISHED, testEvent.getStatus());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void publishEvent_WithNonDraftEvent_ShouldThrowException() {
        testEvent.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(InvalidEventDataException.class, () -> 
            eventService.publishEvent(eventId, organizerId)
        );
    }

    @Test
    void cancelEvent_WithValidEvent_ShouldCancelEvent() {
        testEvent.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.cancelEvent(eventId, organizerId);

        assertNotNull(result);
        assertEquals(EventStatus.CANCELLED, testEvent.getStatus());
        verify(eventRepository).save(testEvent);
    }

    // ========== Event Listing Tests ==========

    @Test
    void getEventsByOrganizer_ShouldReturnOrganizerEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

        when(eventRepository.findByOrganizerId(organizerId, pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = eventService.getEventsByOrganizer(organizerId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).findByOrganizerId(organizerId, pageable);
    }

    @Test
    void getPublishedEvents_ShouldReturnPublishedEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        testEvent.setStatus(EventStatus.PUBLISHED);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

        when(eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = eventService.getPublishedEvents(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).findByStatus(EventStatus.PUBLISHED, pageable);
    }

    @Test
    void getUpcomingEvents_ShouldReturnUpcomingEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class), eq(pageable)))
            .thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = eventService.getUpcomingEvents(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class), eq(pageable));
    }

    // ========== Image Management Tests ==========

    @Test
    void updateEventImage_WithValidData_ShouldUpdateImage() {
        String imageUrl = "https://s3.amazonaws.com/bucket/image.jpg";

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        EventDto result = eventService.updateEventImage(eventId, imageUrl, organizerId);

        assertNotNull(result);
        assertEquals(imageUrl, testEvent.getImageUrl());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void updateEventImage_WithUnauthorizedUser_ShouldThrowException() {
        String imageUrl = "https://s3.amazonaws.com/bucket/image.jpg";
        UUID differentOrganizerId = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(EventAccessDeniedException.class, () -> 
            eventService.updateEventImage(eventId, imageUrl, differentOrganizerId)
        );
    }
}
