package com.eventbooking.ticket.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public class ReserveTicketsRequest {
    
    @NotNull(message = "Ticket type ID is required")
    private UUID ticketTypeId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    // Getters and Setters
    public UUID getTicketTypeId() {
        return ticketTypeId;
    }
    
    public void setTicketTypeId(UUID ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
