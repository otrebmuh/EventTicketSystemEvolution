package com.eventbooking.event.service;

import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.SearchCriteria;
import com.eventbooking.event.dto.SearchSuggestionsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    
    Page<EventDto> searchEvents(SearchCriteria criteria, Pageable pageable);
    
    SearchSuggestionsDto getSearchSuggestions(String query);
}