package com.eventbooking.event.dto;

import java.util.List;

public class SearchSuggestionsDto {
    
    private List<String> events;
    private List<String> venues;
    private List<String> categories;
    private List<String> cities;
    
    // Default constructor
    public SearchSuggestionsDto() {}
    
    // Constructor with all fields
    public SearchSuggestionsDto(List<String> events, List<String> venues, List<String> categories, List<String> cities) {
        this.events = events;
        this.venues = venues;
        this.categories = categories;
        this.cities = cities;
    }
    
    // Getters and setters
    public List<String> getEvents() {
        return events;
    }
    
    public void setEvents(List<String> events) {
        this.events = events;
    }
    
    public List<String> getVenues() {
        return venues;
    }
    
    public void setVenues(List<String> venues) {
        this.venues = venues;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    
    public List<String> getCities() {
        return cities;
    }
    
    public void setCities(List<String> cities) {
        this.cities = cities;
    }
}