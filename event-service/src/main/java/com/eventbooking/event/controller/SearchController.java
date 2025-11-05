package com.eventbooking.event.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.SearchCriteria;
import com.eventbooking.event.dto.SearchSuggestionsDto;
import com.eventbooking.event.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class SearchController {
    
    private final SearchService searchService;
    
    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<EventDto>>> searchEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query);
        criteria.setCity(city);
        criteria.setCategoryId(category);
        criteria.setDateFrom(dateFrom);
        criteria.setDateTo(dateTo);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        
        Page<EventDto> events = searchService.searchEvents(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionsDto>> getSearchSuggestions(
            @RequestParam String query) {
        
        SearchSuggestionsDto suggestions = searchService.getSearchSuggestions(query);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}