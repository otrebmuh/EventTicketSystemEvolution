package com.eventbooking.event.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventNotFoundException extends RuntimeException {
    
    public EventNotFoundException(UUID eventId) {
        super("Event not found with ID: " + eventId);
    }
    
    public EventNotFoundException(String message) {
        super(message);
    }
}