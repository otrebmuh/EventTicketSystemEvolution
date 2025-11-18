package com.eventbooking.event.controller;

import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.entity.Event;
import com.eventbooking.event.mapper.EventMapper;
import com.eventbooking.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/internal")
public class InternalEventController {

    private static final Logger log = LoggerFactory.getLogger(InternalEventController.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID eventId) {
        log.debug("Internal request to fetch event: {}", eventId);
        return eventRepository.findById(eventId)
                .map(eventMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{eventId}/validate-organizer/{userId}")
    public ResponseEntity<Map<String, Object>> validateOrganizer(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        log.debug("Validating if user {} is organizer of event {}", userId, eventId);
        
        boolean isOrganizer = eventRepository.findById(eventId)
                .map(Event::getOrganizerId)
                .map(organizerId -> organizerId.equals(userId))
                .orElse(false);

        return ResponseEntity.ok(Map.of(
                "isOrganizer", isOrganizer,
                "eventId", eventId,
                "userId", userId
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<EventDto>> getEventsByIds(@RequestBody List<UUID> eventIds) {
        log.debug("Internal request to fetch {} events", eventIds.size());
        List<EventDto> events = eventRepository.findAllById(eventIds).stream()
                .map(eventMapper::toDto)
                .toList();
        return ResponseEntity.ok(events);
    }
}
