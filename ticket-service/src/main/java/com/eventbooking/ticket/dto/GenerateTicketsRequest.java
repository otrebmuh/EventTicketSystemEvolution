package com.eventbooking.ticket.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class GenerateTicketsRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotNull(message = "Ticket type ID is required")
    private UUID ticketTypeId;
    
    @NotNull(message = "Quantity is required")
    private Integer quantity;
    
    private String holderName;
    
    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
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
    
    public String getHolderName() {
        return holderName;
    }
    
    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }
}
