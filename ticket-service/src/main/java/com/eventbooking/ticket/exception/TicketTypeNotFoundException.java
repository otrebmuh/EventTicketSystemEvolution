package com.eventbooking.ticket.exception;

import java.util.UUID;

public class TicketTypeNotFoundException extends RuntimeException {
    
    public TicketTypeNotFoundException(UUID ticketTypeId) {
        super("Ticket type not found with id: " + ticketTypeId);
    }
    
    public TicketTypeNotFoundException(String message) {
        super(message);
    }
}
