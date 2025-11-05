package com.eventbooking.event.repository;

import com.eventbooking.event.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    
    // Find venue by name and city (to avoid duplicates)
    Optional<Venue> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
    
    // Find venues by city
    List<Venue> findByCityIgnoreCase(String city);
    
    // Search venues by name
    @Query("SELECT v FROM Venue v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Venue> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find venues within a geographic area (for location-based search)
    @Query("""
        SELECT v FROM Venue v 
        WHERE v.latitude BETWEEN :minLat AND :maxLat 
        AND v.longitude BETWEEN :minLon AND :maxLon
        """)
    List<Venue> findVenuesInArea(
        @Param("minLat") Double minLatitude,
        @Param("maxLat") Double maxLatitude,
        @Param("minLon") Double minLongitude,
        @Param("maxLon") Double maxLongitude
    );
}