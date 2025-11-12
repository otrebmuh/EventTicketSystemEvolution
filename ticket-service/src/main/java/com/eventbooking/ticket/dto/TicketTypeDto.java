package com.eventbooking.ticket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketTypeDto {
    
    private UUID id;
    private UUID eventId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantityAvailable;
    private Integer quantitySold;
    private Integer availableQuantity;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Integer perPersonLimit;
    private String venueZone;
    private Boolean isOnSale;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
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
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
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
    
    public Boolean getIsOnSale() {
        return isOnSale;
    }
    
    public void setIsOnSale(Boolean isOnSale) {
        this.isOnSale = isOnSale;
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
}
