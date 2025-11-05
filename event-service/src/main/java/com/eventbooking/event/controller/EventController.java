package com.eventbooking.event.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.common.util.JwtUtil;
import com.eventbooking.event.dto.CreateEventRequest;
import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.UpdateEventRequest;
import com.eventbooking.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {
    
    private final EventService eventService;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public EventController(EventService eventService, JwtUtil jwtUtil) {
        this.eventService = eventService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<EventDto>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.createEvent(request, organizerId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(event));
    }
    
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDto>> getEvent(@PathVariable UUID eventId) {
        EventDto event = eventService.getEventById(eventId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }
    
    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDto>> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.updateEvent(eventId, request, organizerId);
        
        return ResponseEntity.ok(ApiResponse.success(event));
    }
    
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        eventService.deleteEvent(eventId, organizerId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventDto>>> getPublishedEvents(Pageable pageable) {
        Page<EventDto> events = eventService.getPublishedEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<Page<EventDto>>> getUpcomingEvents(Pageable pageable) {
        Page<EventDto> events = eventService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @GetMapping("/organizer")
    public ResponseEntity<ApiResponse<Page<EventDto>>> getOrganizerEvents(
            @RequestHeader("Authorization") String authHeader,
            Pageable pageable) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        Page<EventDto> events = eventService.getEventsByOrganizer(organizerId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @PostMapping("/{eventId}/publish")
    public ResponseEntity<ApiResponse<EventDto>> publishEvent(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.publishEvent(eventId, organizerId);
        
        return ResponseEntity.ok(ApiResponse.success(event));
    }
    
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<ApiResponse<EventDto>> cancelEvent(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authHeader) {
        
        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.cancelEvent(eventId, organizerId);
        
        return ResponseEntity.ok(ApiResponse.success(event));
    }
    
    // Internal API for other services
    @GetMapping("/internal/{eventId}")
    public ResponseEntity<EventDto> getEventForTicketService(@PathVariable UUID eventId) {
        EventDto event = eventService.getEventForTicketService(eventId);
        return ResponseEntity.ok(event);
    }
    
    private UUID extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}