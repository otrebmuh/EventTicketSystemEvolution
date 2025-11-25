package com.eventbooking.event.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.common.util.JwtUtil;
import com.eventbooking.event.dto.CreateEventRequest;
import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.ImageUploadResponse;
import com.eventbooking.event.dto.UpdateEventRequest;
import com.eventbooking.event.service.EventService;
import com.eventbooking.event.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final ImageService imageService;
    private final JwtUtil jwtUtil;

    @Autowired
    public EventController(EventService eventService, ImageService imageService, JwtUtil jwtUtil) {
        this.eventService = eventService;
        this.imageService = imageService;
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
    public ResponseEntity<ApiResponse<EventDto>> getEvent(@PathVariable("eventId") UUID eventId) {
        EventDto event = eventService.getEventById(eventId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDto>> updateEvent(
            @PathVariable("eventId") UUID eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("Authorization") String authHeader) {

        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.updateEvent(eventId, request, organizerId);

        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable("eventId") UUID eventId,
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
            @PathVariable("eventId") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {

        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.publishEvent(eventId, organizerId);

        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<ApiResponse<EventDto>> cancelEvent(
            @PathVariable("eventId") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {

        UUID organizerId = extractUserIdFromToken(authHeader);
        EventDto event = eventService.cancelEvent(eventId, organizerId);

        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping(value = "/{eventId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadEventImage(
            @PathVariable("eventId") UUID eventId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        UUID organizerId = extractUserIdFromToken(authHeader);

        // Upload image to S3
        String s3Url = imageService.uploadEventImage(image, eventId);

        // Update event with image URL
        EventDto event = eventService.updateEventImage(eventId, s3Url, organizerId);

        // Get CDN URL
        String cdnUrl = imageService.getCdnUrl(s3Url);

        ImageUploadResponse response = new ImageUploadResponse(
                s3Url,
                cdnUrl,
                image.getOriginalFilename(),
                image.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{eventId}/image")
    public ResponseEntity<ApiResponse<EventDto>> deleteEventImage(
            @PathVariable("eventId") UUID eventId,
            @RequestHeader("Authorization") String authHeader) {

        UUID organizerId = extractUserIdFromToken(authHeader);

        // Get current event to retrieve image URL
        EventDto event = eventService.getEventById(eventId);

        // Delete image from S3 if exists
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            imageService.deleteEventImage(event.getImageUrl());
        }

        // Update event to remove image URL
        EventDto updatedEvent = eventService.updateEventImage(eventId, null, organizerId);

        return ResponseEntity.ok(ApiResponse.success(updatedEvent));
    }

    private UUID extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}