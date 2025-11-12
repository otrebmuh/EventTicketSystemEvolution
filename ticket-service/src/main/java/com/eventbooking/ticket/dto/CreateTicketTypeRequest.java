package com.eventbooking.ticket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreateTicketTypeRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotBlank(message = "Ticket type name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;
    
    @NotNull(message = "Quantity available is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityAvailable;
    
    private LocalDateTime saleStartDate;
    
    private LocalDateTime saleEndDate;
    
    @Min(value = 1, message = "Per person limit must be at least 1")
    @Max(value = 100, message = "Per person limit must not exceed 100")
    private Integer perPersonLimit = 10;
    
    @Size(max = 100, message = "Venue zone must not exceed 100 characters")
    private String venueZone;
    
    // Getters and Setters
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
