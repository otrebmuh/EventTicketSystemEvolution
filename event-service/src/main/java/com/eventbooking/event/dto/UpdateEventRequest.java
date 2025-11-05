package com.eventbooking.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UpdateEventRequest {
    
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    private String name;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;
    
    @Valid
    private VenueRequest venue;
    
    private UUID categoryId;
    
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 100000, message = "Max capacity cannot exceed 100,000")
    private Integer maxCapacity;
    
    private List<String> tags;
    
    // Default constructor
    public UpdateEventRequest() {}
    
    // Getters and setters
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
    
    public LocalDateTime getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }
    
    public VenueRequest getVenue() {
        return venue;
    }
    
    public void setVenue(VenueRequest venue) {
        this.venue = venue;
    }
    
    public UUID getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
    
    public Integer getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}