package com.eventbooking.notification.service;

import com.eventbooking.notification.dto.TicketDeliveryRequest;
import com.eventbooking.notification.dto.TicketDeliveryResponse;

public interface TicketDeliveryService {
    
    /**
     * Deliver tickets to the user through specified channels
     * @param request The ticket delivery request
     * @return The delivery response with status and links
     */
    TicketDeliveryResponse deliverTickets(TicketDeliveryRequest request);
    
    /**
     * Generate a mobile-friendly web link for a ticket
     * @param ticketId The ticket ID
     * @return The web link URL
     */
    String generateTicketWebLink(java.util.UUID ticketId);
    
    /**
     * Generate a calendar event link for an event
     * @param eventName The event name
     * @param eventDate The event date
     * @param venueName The venue name
     * @param venueAddress The venue address
     * @return The calendar event link (iCal format)
     */
    String generateCalendarEventLink(String eventName, String eventDate, String venueName, String venueAddress);
}
