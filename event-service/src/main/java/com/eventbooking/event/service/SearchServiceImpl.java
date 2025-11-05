package com.eventbooking.event.service;

import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.SearchCriteria;
import com.eventbooking.event.dto.SearchSuggestionsDto;
import com.eventbooking.event.entity.Event;
import com.eventbooking.event.mapper.EventMapper;
import com.eventbooking.event.repository.EventCategoryRepository;
import com.eventbooking.event.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {
    
    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final CacheService cacheService;
    
    @Autowired
    public SearchServiceImpl(
            EventRepository eventRepository,
            EventCategoryRepository categoryRepository,
            EventMapper eventMapper,
            CacheService cacheService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.eventMapper = eventMapper;
        this.cacheService = cacheService;
    }
    
    @Override
    public Page<EventDto> searchEvents(SearchCriteria criteria, Pageable pageable) {
        String cacheKey = cacheService.generateSearchCacheKey(criteria.toString() + pageable.toString());
        
        // Check cache first
        Page<EventDto> cachedResults = cacheService.getCachedSearchResults(cacheKey, EventDto.class);
        if (cachedResults != null) {
            return cachedResults;
        }
        
        // Execute search query
        Page<Event> events = eventRepository.searchEvents(
            criteria.getQuery(),
            criteria.getCity(),
            criteria.getCategoryId(),
            criteria.getDateFrom(),
            criteria.getDateTo(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            pageable
        );
        
        Page<EventDto> results = events.map(eventMapper::toDto);
        
        // Cache results
        cacheService.cacheSearchResults(cacheKey, results);
        
        return results;
    }
    
    @Override
    public SearchSuggestionsDto getSearchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchSuggestionsDto();
        }
        
        String trimmedQuery = query.trim();
        Pageable limit = PageRequest.of(0, 5); // Limit to 5 suggestions each
        
        // Get suggestions from different sources
        List<String> eventSuggestions = eventRepository.findEventNameSuggestions(trimmedQuery, limit);
        List<String> venueSuggestions = eventRepository.findVenueNameSuggestions(trimmedQuery, limit);
        List<String> categorySuggestions = categoryRepository.findCategoryNameSuggestions(trimmedQuery, limit);
        List<String> citySuggestions = eventRepository.findCitySuggestions(trimmedQuery, limit);
        
        return new SearchSuggestionsDto(eventSuggestions, venueSuggestions, categorySuggestions, citySuggestions);
    }
}