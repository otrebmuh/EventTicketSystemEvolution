package com.eventbooking.event.repository;

import com.eventbooking.event.entity.Event;
import com.eventbooking.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    // Find events by organizer
    Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);
    
    // Find published events
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    
    // Optimized search query with proper indexing
    @Query("""
        SELECT e FROM Event e 
        LEFT JOIN FETCH e.venue v 
        LEFT JOIN FETCH e.category c 
        WHERE (:query IS NULL OR 
               LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))) 
        AND (:city IS NULL OR LOWER(v.city) = LOWER(:city)) 
        AND (:categoryId IS NULL OR c.id = :categoryId) 
        AND (:dateFrom IS NULL OR e.eventDate >= :dateFrom) 
        AND (:dateTo IS NULL OR e.eventDate <= :dateTo) 
        AND (:minPrice IS NULL OR e.minPrice >= :minPrice) 
        AND (:maxPrice IS NULL OR e.maxPrice <= :maxPrice) 
        AND e.status = 'PUBLISHED'
        """)
    Page<Event> searchEvents(
        @Param("query") String query,
        @Param("city") String city,
        @Param("categoryId") UUID categoryId,
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo") LocalDateTime dateTo,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Find events by category
    Page<Event> findByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);
    
    // Find events by city
    @Query("SELECT e FROM Event e JOIN e.venue v WHERE LOWER(v.city) = LOWER(:city) AND e.status = :status")
    Page<Event> findByCityAndStatus(@Param("city") String city, @Param("status") EventStatus status, Pageable pageable);
    
    // Find upcoming events
    @Query("SELECT e FROM Event e WHERE e.eventDate > :now AND e.status = 'PUBLISHED' ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);
    
    // Search suggestions for autocomplete
    @Query("""
        SELECT DISTINCT e.name FROM Event e 
        WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) 
        AND e.status = 'PUBLISHED'
        """)
    List<String> findEventNameSuggestions(@Param("query") String query, Pageable pageable);
    
    @Query("""
        SELECT DISTINCT v.name FROM Event e 
        JOIN e.venue v 
        WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%')) 
        AND e.status = 'PUBLISHED'
        """)
    List<String> findVenueNameSuggestions(@Param("query") String query, Pageable pageable);
    
    @Query("""
        SELECT DISTINCT v.city FROM Event e 
        JOIN e.venue v 
        WHERE LOWER(v.city) LIKE LOWER(CONCAT('%', :query, '%')) 
        AND e.status = 'PUBLISHED'
        """)
    List<String> findCitySuggestions(@Param("query") String query, Pageable pageable);
}