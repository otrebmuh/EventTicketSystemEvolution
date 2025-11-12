package com.eventbooking.ticket.exception;

public class InsufficientInventoryException extends RuntimeException {
    
    public InsufficientInventoryException(String message) {
        super(message);
    }
    
    public InsufficientInventoryException(Integer requested, Integer available) {
        super(String.format("Insufficient inventory. Requested: %d, Available: %d", requested, available));
    }
}
