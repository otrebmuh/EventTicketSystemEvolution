package com.eventbooking.ticket.service;

import com.eventbooking.ticket.dto.CreateTicketTypeRequest;
import com.eventbooking.ticket.dto.ReserveTicketsRequest;
import com.eventbooking.ticket.dto.ReservationDto;
import com.eventbooking.ticket.dto.TicketTypeDto;
import com.eventbooking.ticket.dto.UpdateTicketTypeRequest;

import java.util.List;
import java.util.UUID;

public interface TicketTypeService {
    
    /**
     * Create a new ticket type for an event
     */
    TicketTypeDto createTicketType(CreateTicketTypeRequest request, UUID organizerId);
    
    /**
     * Get ticket type by ID
     */
    TicketTypeDto getTicketTypeById(UUID ticketTypeId);
    
    /**
     * Get all ticket types for an event
     */
    List<TicketTypeDto> getTicketTypesByEventId(UUID eventId);
    
    /**
     * Get available ticket types for an event (on sale)
     */
    List<TicketTypeDto> getAvailableTicketTypesByEventId(UUID eventId);
    
    /**
     * Update ticket type
     */
    TicketTypeDto updateTicketType(UUID ticketTypeId, UpdateTicketTypeRequest request, UUID organizerId);
    
    /**
     * Delete ticket type
     */
    void deleteTicketType(UUID ticketTypeId, UUID organizerId);
    
    /**
     * Reserve tickets for a user
     */
    ReservationDto reserveTickets(ReserveTicketsRequest request, UUID userId);
    
    /**
     * Cancel a reservation
     */
    void cancelReservation(UUID reservationId, UUID userId);
    
    /**
     * Get user's active reservations
     */
    List<ReservationDto> getUserActiveReservations(UUID userId);
    
    /**
     * Clean up expired reservations
     */
    void cleanupExpiredReservations();
}
