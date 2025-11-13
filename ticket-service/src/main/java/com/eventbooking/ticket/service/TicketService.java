package com.eventbooking.ticket.service;

import com.eventbooking.ticket.dto.GenerateTicketsRequest;
import com.eventbooking.ticket.dto.TicketDto;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    
    /**
     * Generate tickets for a completed order
     * @param request The ticket generation request
     * @return List of generated tickets
     */
    List<TicketDto> generateTickets(GenerateTicketsRequest request);
    
    /**
     * Get a ticket by its ID
     * @param ticketId The ticket ID
     * @return The ticket DTO
     */
    TicketDto getTicketById(UUID ticketId);
    
    /**
     * Get a ticket by its ticket number
     * @param ticketNumber The ticket number
     * @return The ticket DTO
     */
    TicketDto getTicketByNumber(String ticketNumber);
    
    /**
     * Get all tickets for an order
     * @param orderId The order ID
     * @return List of tickets
     */
    List<TicketDto> getTicketsByOrderId(UUID orderId);
    
    /**
     * Get all tickets for a user (by retrieving all their orders)
     * @param userId The user ID
     * @return List of tickets
     */
    List<TicketDto> getTicketsByUserId(UUID userId);
    
    /**
     * Cancel a ticket
     * @param ticketId The ticket ID
     */
    void cancelTicket(UUID ticketId);
    
    /**
     * Validate a ticket by QR code
     * @param qrCode The QR code data
     * @return The ticket DTO if valid
     */
    TicketDto validateTicket(String qrCode);
}
