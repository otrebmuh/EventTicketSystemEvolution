package com.eventbooking.ticket.exception;

public class InvalidReservationException extends RuntimeException {
    
    public InvalidReservationException(String message) {
        super(message);
    }
}
