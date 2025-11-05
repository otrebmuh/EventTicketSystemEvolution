package com.eventbooking.event.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class EventAccessDeniedException extends RuntimeException {
    
    public EventAccessDeniedException(String message) {
        super(message);
    }
}