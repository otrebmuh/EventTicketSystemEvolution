package com.eventbooking.event.service;

import com.eventbooking.event.dto.EventDto;
import com.eventbooking.event.dto.SearchCriteria;
import com.eventbooking.event.dto.SearchSuggestionsDto;
import com.eventbooking.event.entity.Event;
import com.eventbooking.event.entity.EventCategory;
import com.eventbooking.event.entity.EventStatus;
import com.eventbooking.event.entity.Venue;
import com.eventbooking.event.mapper.EventMapper;
import com.eventbooking.event.repository.EventCategoryRepository;
import com.eventbooking.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCategoryRepository categoryRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Event testEvent;
    private EventDto testEventDto;
    private Venue testVenue;
    private EventCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new EventCategory();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Music");

        testVenue = new Venue();
        testVenue.setId(UUID.randomUUID());
        testVenue.setName("Madison Square Garden");
        testVenue.setCity("New York");
        testVenue.setAddress("4 Pennsylvania Plaza");
        testVenue.setState("NY");
        testVenue.setZipCode("10001");
        testVenue.setCountry("USA");

        testEvent = new Event();
        testEvent.setId(UUID.randomUUID());
        testEvent.setOrganizerId(UUID.randomUUID());
        testEvent.setName("Rock Concert");
        testEvent.setDescription("Amazing rock concert");
        testEvent.setEventDate(LocalDateTime.now().plusDays(30));
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setMinPrice(BigDecimal.valueOf(50.00));
        testEvent.setMaxPrice(BigDecimal.valueOf(150.00));

        testEventDto = new EventDto();
        testEventDto.setId(testEvent.getId());
        testEventDto.setName(testEvent.getName());
    }

    // ========== Search Tests ==========

    @Test
    void searchEvents_WithCachedResults_ShouldReturnFromCache() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("concert");
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<EventDto> cachedPage = new PageImpl<>(Collections.singletonList(testEventDto));
        String cacheKey = "search_key";

        when(cacheService.generateSearchCacheKey(anyString())).thenReturn(cacheKey);
        when(cacheService.getCachedSearchResults(cacheKey, EventDto.class)).thenReturn(cachedPage);

        Page<EventDto> result = searchService.searchEvents(criteria, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository, never()).searchEvents(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchEvents_WithoutCache_ShouldFetchFromDatabase() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("concert");
        criteria.setCity("New York");
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));
        String cacheKey = "search_key";

        when(cacheService.generateSearchCacheKey(anyString())).thenReturn(cacheKey);
        when(cacheService.getCachedSearchResults(cacheKey, EventDto.class)).thenReturn(null);
        when(eventRepository.searchEvents(
            eq("concert"),
            eq("New York"),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)
        )).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = searchService.searchEvents(criteria, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).searchEvents(any(), any(), any(), any(), any(), any(), any(), any());
        verify(cacheService).cacheSearchResults(eq(cacheKey), any());
    }

    @Test
    void searchEvents_WithAllCriteria_ShouldApplyAllFilters() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("concert");
        criteria.setCity("New York");
        criteria.setCategoryId(testCategory.getId());
        criteria.setDateFrom(LocalDateTime.now());
        criteria.setDateTo(LocalDateTime.now().plusMonths(1));
        criteria.setMinPrice(BigDecimal.valueOf(50.00));
        criteria.setMaxPrice(BigDecimal.valueOf(100.00));
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));
        String cacheKey = "search_key";

        when(cacheService.generateSearchCacheKey(anyString())).thenReturn(cacheKey);
        when(cacheService.getCachedSearchResults(cacheKey, EventDto.class)).thenReturn(null);
        when(eventRepository.searchEvents(
            eq("concert"),
            eq("New York"),
            eq(testCategory.getId()),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(BigDecimal.valueOf(50.00)),
            eq(BigDecimal.valueOf(100.00)),
            eq(pageable)
        )).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = searchService.searchEvents(criteria, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository).searchEvents(
            eq("concert"),
            eq("New York"),
            eq(testCategory.getId()),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(BigDecimal.valueOf(50.00)),
            eq(BigDecimal.valueOf(100.00)),
            eq(pageable)
        );
    }

    @Test
    void searchEvents_WithEmptyCriteria_ShouldReturnAllPublishedEvents() {
        SearchCriteria criteria = new SearchCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));
        String cacheKey = "search_key";

        when(cacheService.generateSearchCacheKey(anyString())).thenReturn(cacheKey);
        when(cacheService.getCachedSearchResults(cacheKey, EventDto.class)).thenReturn(null);
        when(eventRepository.searchEvents(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)
        )).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(testEventDto);

        Page<EventDto> result = searchService.searchEvents(criteria, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ========== Search Suggestions Tests ==========

    @Test
    void getSearchSuggestions_WithValidQuery_ShouldReturnSuggestions() {
        String query = "rock";
        Pageable limit = PageRequest.of(0, 5);

        List<String> eventSuggestions = Arrays.asList("Rock Concert", "Rock Festival");
        List<String> venueSuggestions = Arrays.asList("Rockefeller Center");
        List<String> categorySuggestions = Arrays.asList("Rock Music");
        List<String> citySuggestions = Arrays.asList("Rochester");

        when(eventRepository.findEventNameSuggestions(query, limit)).thenReturn(eventSuggestions);
        when(eventRepository.findVenueNameSuggestions(query, limit)).thenReturn(venueSuggestions);
        when(categoryRepository.findCategoryNameSuggestions(query, limit)).thenReturn(categorySuggestions);
        when(eventRepository.findCitySuggestions(query, limit)).thenReturn(citySuggestions);

        SearchSuggestionsDto result = searchService.getSearchSuggestions(query);

        assertNotNull(result);
        assertEquals(2, result.getEvents().size());
        assertEquals(1, result.getVenues().size());
        assertEquals(1, result.getCategories().size());
        assertEquals(1, result.getCities().size());
        
        verify(eventRepository).findEventNameSuggestions(query, limit);
        verify(eventRepository).findVenueNameSuggestions(query, limit);
        verify(categoryRepository).findCategoryNameSuggestions(query, limit);
        verify(eventRepository).findCitySuggestions(query, limit);
    }

    @Test
    void getSearchSuggestions_WithEmptyQuery_ShouldReturnEmptySuggestions() {
        SearchSuggestionsDto result = searchService.getSearchSuggestions("");

        assertNotNull(result);
        assertTrue(result.getEvents() == null || result.getEvents().isEmpty());
        
        verify(eventRepository, never()).findEventNameSuggestions(any(), any());
    }

    @Test
    void getSearchSuggestions_WithNullQuery_ShouldReturnEmptySuggestions() {
        SearchSuggestionsDto result = searchService.getSearchSuggestions(null);

        assertNotNull(result);
        assertTrue(result.getEvents() == null || result.getEvents().isEmpty());
        
        verify(eventRepository, never()).findEventNameSuggestions(any(), any());
    }

    @Test
    void getSearchSuggestions_WithWhitespaceQuery_ShouldReturnEmptySuggestions() {
        SearchSuggestionsDto result = searchService.getSearchSuggestions("   ");

        assertNotNull(result);
        assertTrue(result.getEvents() == null || result.getEvents().isEmpty());
        
        verify(eventRepository, never()).findEventNameSuggestions(any(), any());
    }

    @Test
    void getSearchSuggestions_WithPartialMatch_ShouldReturnMatchingSuggestions() {
        String query = "con";
        Pageable limit = PageRequest.of(0, 5);

        List<String> eventSuggestions = Arrays.asList("Concert", "Conference");
        List<String> venueSuggestions = Arrays.asList("Convention Center");
        List<String> categorySuggestions = Collections.emptyList();
        List<String> citySuggestions = Collections.emptyList();

        when(eventRepository.findEventNameSuggestions(query, limit)).thenReturn(eventSuggestions);
        when(eventRepository.findVenueNameSuggestions(query, limit)).thenReturn(venueSuggestions);
        when(categoryRepository.findCategoryNameSuggestions(query, limit)).thenReturn(categorySuggestions);
        when(eventRepository.findCitySuggestions(query, limit)).thenReturn(citySuggestions);

        SearchSuggestionsDto result = searchService.getSearchSuggestions(query);

        assertNotNull(result);
        assertEquals(2, result.getEvents().size());
        assertEquals(1, result.getVenues().size());
        assertTrue(result.getCategories().isEmpty());
        assertTrue(result.getCities().isEmpty());
    }
}
