package com.eventbooking.event.mapper;

import com.eventbooking.event.dto.CategoryDto;
import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.VenueDto;
import com.eventbooking.event.entity.Event;
import com.eventbooking.event.entity.EventCategory;
import com.eventbooking.event.entity.Venue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventMapper {
    
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public EventDto toDto(Event event) {
        if (event == null) {
            return null;
        }
        
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setOrganizerId(event.getOrganizerId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setVenue(toVenueDto(event.getVenue()));
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setImageUrl(event.getImageUrl());
        dto.setStatus(event.getStatus());
        dto.setMaxCapacity(event.getMaxCapacity());
        dto.setMinPrice(event.getMinPrice());
        dto.setMaxPrice(event.getMaxPrice());
        dto.setTags(parseTagsFromJson(event.getTags()));
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        
        return dto;
    }
    
    public VenueDto toVenueDto(Venue venue) {
        if (venue == null) {
            return null;
        }
        
        VenueDto dto = new VenueDto();
        dto.setId(venue.getId());
        dto.setName(venue.getName());
        dto.setAddress(venue.getAddress());
        dto.setCity(venue.getCity());
        dto.setState(venue.getState());
        dto.setZipCode(venue.getZipCode());
        dto.setCountry(venue.getCountry());
        dto.setLatitude(venue.getLatitude());
        dto.setLongitude(venue.getLongitude());
        dto.setMaxCapacity(venue.getMaxCapacity());
        dto.setVenueType(venue.getVenueType());
        dto.setCreatedAt(venue.getCreatedAt());
        
        return dto;
    }
    
    public CategoryDto toCategoryDto(EventCategory category) {
        if (category == null) {
            return null;
        }
        
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIconUrl(category.getIconUrl());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        
        return dto;
    }
    
    private List<String> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            // Return null if JSON parsing fails
            return null;
        }
    }
}