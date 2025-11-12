package com.eventbooking.ticket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateTicketTypeRequest {
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;
    
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantityAvailable;
    
    private LocalDateTime saleStartDate;
    
    private LocalDateTime saleEndDate;
    
    @Min(value = 1, message = "Per person limit must be at least 1")
    @Max(value = 100, message = "Per person limit must not exceed 100")
    private Integer perPersonLimit;
    
    @Size(max = 100, message = "Venue zone must not exceed 100 characters")
    private String venueZone;
    
    // Getters and Setters
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
}
