package com.eventbooking.event.repository;

import com.eventbooking.event.entity.EventCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {
    
    // Find active categories ordered by display order
    List<EventCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    // Find category by name
    Optional<EventCategory> findByNameIgnoreCase(String name);
    
    // Check if category name exists (for validation)
    boolean existsByNameIgnoreCase(String name);
    
    // Search categories for suggestions
    @Query("""
        SELECT DISTINCT c.name FROM EventCategory c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) 
        AND c.isActive = true
        """)
    List<String> findCategoryNameSuggestions(@Param("query") String query, Pageable pageable);
}