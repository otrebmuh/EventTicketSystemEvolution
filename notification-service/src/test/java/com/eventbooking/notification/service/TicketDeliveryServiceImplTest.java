package com.eventbooking.notification.service;

import com.eventbooking.notification.dto.TicketDeliveryRequest;
import com.eventbooking.notification.dto.TicketDeliveryResponse;
import com.eventbooking.notification.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDeliveryServiceImplTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TicketDeliveryServiceImpl ticketDeliveryService;

    private UUID userId;
    private UUID orderId;
    private List<UUID> ticketIds;
    private TicketDeliveryRequest deliveryRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        ticketIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

        deliveryRequest = new TicketDeliveryRequest();
        deliveryRequest.setUserId(userId);
        deliveryRequest.setOrderId(orderId);
        deliveryRequest.setRecipientEmail("user@example.com");
        deliveryRequest.setTicketIds(ticketIds);
        deliveryRequest.setChannel(NotificationChannel.EMAIL);
        deliveryRequest.setIncludeCalendarEvent(true);
        deliveryRequest.setGenerateWebLink(true);
    }

    @Test
    void deliverTickets_EmailChannel_Success() {
        TicketDeliveryResponse response = ticketDeliveryService.deliverTickets(deliveryRequest);

        assertNotNull(response);
        assertNotNull(response.getDeliveryStatus());
        assertNotNull(response.getTicketWebLinks());
        assertEquals(ticketIds.size(), response.getTicketWebLinks().size());
        assertNotNull(response.getCalendarEventLink());
    }

    @Test
    void deliverTickets_WithSMSChannel() {
        deliveryRequest.setChannel(NotificationChannel.SMS);

        TicketDeliveryResponse response = ticketDeliveryService.deliverTickets(deliveryRequest);

        assertNotNull(response);
        assertNotNull(response.getDeliveryStatus());
        assertNotNull(response.getTicketWebLinks());
    }

    @Test
    void generateTicketWebLink_Success() {
        UUID ticketId = UUID.randomUUID();

        String webLink = ticketDeliveryService.generateTicketWebLink(ticketId);

        assertNotNull(webLink);
        assertTrue(webLink.contains(ticketId.toString()));
        assertTrue(webLink.startsWith("https://"));
    }

    @Test
    void generateCalendarEventLink_Success() {
        String eventName = "Summer Music Festival";
        String eventDate = "2024-07-15T18:00:00";
        String venueName = "Central Park";
        String venueAddress = "123 Park Ave, New York, NY";

        String calendarLink = ticketDeliveryService.generateCalendarEventLink(
            eventName, eventDate, venueName, venueAddress);

        assertNotNull(calendarLink);
        assertTrue(calendarLink.contains("BEGIN:VCALENDAR"));
        assertTrue(calendarLink.contains("Summer Music Festival"));
        assertTrue(calendarLink.contains("Central Park"));
    }

    @Test
    void generateCalendarEventLink_HandlesSpecialCharacters() {
        String eventName = "Rock & Roll Festival";
        String eventDate = "2024-07-15T18:00:00";
        String venueName = "O'Brien's Venue";
        String venueAddress = "123 Main St, City";

        String calendarLink = ticketDeliveryService.generateCalendarEventLink(
            eventName, eventDate, venueName, venueAddress);

        assertNotNull(calendarLink);
        assertTrue(calendarLink.contains("BEGIN:VCALENDAR"));
    }

    @Test
    void deliverTickets_GeneratesWebLinksForAllTickets() {
        TicketDeliveryResponse response = ticketDeliveryService.deliverTickets(deliveryRequest);

        assertNotNull(response.getTicketWebLinks());
        assertEquals(ticketIds.size(), response.getTicketWebLinks().size());
        
        for (String webLink : response.getTicketWebLinks()) {
            assertNotNull(webLink);
            assertTrue(webLink.startsWith("https://"));
        }
    }

    @Test
    void deliverTickets_WithoutCalendarEvent() {
        deliveryRequest.setIncludeCalendarEvent(false);

        TicketDeliveryResponse response = ticketDeliveryService.deliverTickets(deliveryRequest);

        assertNotNull(response);
        assertNotNull(response.getTicketWebLinks());
    }

    @Test
    void deliverTickets_WithoutWebLinks() {
        deliveryRequest.setGenerateWebLink(false);

        TicketDeliveryResponse response = ticketDeliveryService.deliverTickets(deliveryRequest);

        assertNotNull(response);
        assertNotNull(response.getDeliveryStatus());
    }
}
