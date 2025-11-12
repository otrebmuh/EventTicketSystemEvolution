package com.eventbooking.ticket.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_types", indexes = {
    @Index(name = "idx_ticket_type_event_id", columnList = "event_id"),
    @Index(name = "idx_ticket_type_sale_dates", columnList = "sale_start_date,sale_end_date")
})
public class TicketType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;
    
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;
    
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;
    
    @Column(name = "sale_start_date")
    private LocalDateTime saleStartDate;
    
    @Column(name = "sale_end_date")
    private LocalDateTime saleEndDate;
    
    @Column(name = "per_person_limit")
    private Integer perPersonLimit = 10;
    
    @Column(name = "venue_zone", length = 100)
    private String venueZone;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }
    
    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
    
    public Integer getQuantitySold() {
        return quantitySold;
    }
    
    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }
    
    public Integer getQuantityReserved() {
        return quantityReserved;
    }
    
    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }
    
    public LocalDateTime getSaleStartDate() {
        return saleStartDate;
    }
    
    public void setSaleStartDate(LocalDateTime saleStartDate) {
        this.saleStartDate = saleStartDate;
    }
    
    public LocalDateTime getSaleEndDate() {
        return saleEndDate;
    }
    
    public void setSaleEndDate(LocalDateTime saleEndDate) {
        this.saleEndDate = saleEndDate;
    }
    
    public Integer getPerPersonLimit() {
        return perPersonLimit;
    }
    
    public void setPerPersonLimit(Integer perPersonLimit) {
        this.perPersonLimit = perPersonLimit;
    }
    
    public String getVenueZone() {
        return venueZone;
    }
    
    public void setVenueZone(String venueZone) {
        this.venueZone = venueZone;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getAvailableQuantity() {
        return quantityAvailable - quantitySold - quantityReserved;
    }
    
    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = saleStartDate == null || now.isAfter(saleStartDate) || now.isEqual(saleStartDate);
        boolean beforeEnd = saleEndDate == null || now.isBefore(saleEndDate);
        return afterStart && beforeEnd;
    }
}
