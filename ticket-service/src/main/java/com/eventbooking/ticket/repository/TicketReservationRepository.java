package com.eventbooking.ticket.repository;

import com.eventbooking.ticket.entity.TicketReservation;
import com.eventbooking.ticket.entity.TicketReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketReservationRepository extends JpaRepository<TicketReservation, UUID> {
    
    List<TicketReservation> findByUserIdAndStatus(UUID userId, ReservationStatus status);
    
    List<TicketReservation> findByTicketTypeIdAndStatus(UUID ticketTypeId, ReservationStatus status);
    
    @Query("SELECT r FROM TicketReservation r WHERE r.status = :status AND r.reservedUntil < :currentTime")
    List<TicketReservation> findExpiredReservations(
        @Param("status") ReservationStatus status,
        @Param("currentTime") LocalDateTime currentTime
    );
    
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM TicketReservation r " +
           "WHERE r.ticketTypeId = :ticketTypeId AND r.status = 'ACTIVE' AND r.reservedUntil > :currentTime")
    Integer sumActiveReservationsByTicketTypeId(
        @Param("ticketTypeId") UUID ticketTypeId,
        @Param("currentTime") LocalDateTime currentTime
    );
}
