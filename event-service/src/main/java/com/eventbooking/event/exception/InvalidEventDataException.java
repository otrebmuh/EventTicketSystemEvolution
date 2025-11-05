package com.eventbooking.event.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEventDataException extends RuntimeException {
    
    public InvalidEventDataException(String message) {
        super(message);
    }
}