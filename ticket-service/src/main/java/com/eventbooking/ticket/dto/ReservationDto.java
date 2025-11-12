package com.eventbooking.ticket.dto;

import com.eventbooking.ticket.entity.TicketReservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReservationDto {
    
    private UUID id;
    private UUID userId;
    private UUID ticketTypeId;
    private Integer quantity;
    private LocalDateTime reservedUntil;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
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
    
    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }
    
    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
