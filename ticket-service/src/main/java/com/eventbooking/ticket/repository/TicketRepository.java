package com.eventbooking.ticket.repository;

import com.eventbooking.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Optional<Ticket> findByQrCode(String qrCode);

    List<Ticket> findByOrderId(UUID orderId);

    List<Ticket> findByUserId(UUID userId);

    List<Ticket> findByTicketTypeId(UUID ticketTypeId);

    boolean existsByTicketNumber(String ticketNumber);

    boolean existsByQrCode(String qrCode);
}
