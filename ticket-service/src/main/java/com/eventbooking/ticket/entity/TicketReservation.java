package com.eventbooking.ticket.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_reservations", indexes = {
    @Index(name = "idx_reservation_user_id", columnList = "user_id"),
    @Index(name = "idx_reservation_ticket_type_id", columnList = "ticket_type_id"),
    @Index(name = "idx_reservation_reserved_until", columnList = "reserved_until"),
    @Index(name = "idx_reservation_status", columnList = "status")
})
public class TicketReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
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
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(reservedUntil);
    }
    
    public enum ReservationStatus {
        ACTIVE,
        COMPLETED,
        EXPIRED,
        CANCELLED
    }
}
