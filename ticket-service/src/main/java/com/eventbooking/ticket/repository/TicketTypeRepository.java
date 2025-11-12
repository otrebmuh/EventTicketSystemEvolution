package com.eventbooking.ticket.repository;

import com.eventbooking.ticket.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    
    List<TicketType> findByEventId(UUID eventId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdWithLock(@Param("id") UUID id);
    
    @Query("SELECT t FROM TicketType t WHERE t.eventId = :eventId AND " +
           "(t.saleStartDate IS NULL OR t.saleStartDate <= CURRENT_TIMESTAMP) AND " +
           "(t.saleEndDate IS NULL OR t.saleEndDate > CURRENT_TIMESTAMP)")
    List<TicketType> findAvailableTicketTypesByEventId(@Param("eventId") UUID eventId);
}
