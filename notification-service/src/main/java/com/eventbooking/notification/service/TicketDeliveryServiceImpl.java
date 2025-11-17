package com.eventbooking.notification.service;

import com.eventbooking.notification.dto.TicketDeliveryRequest;
import com.eventbooking.notification.dto.TicketDeliveryResponse;
import com.eventbooking.notification.entity.Notification;
import com.eventbooking.notification.entity.NotificationChannel;
import com.eventbooking.notification.entity.NotificationStatus;
import com.eventbooking.notification.entity.NotificationTemplate;
import com.eventbooking.notification.exception.NotificationNotFoundException;
import com.eventbooking.notification.repository.NotificationRepository;
import com.eventbooking.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketDeliveryServiceImpl implements TicketDeliveryService {
    
    private static final Logger log = LoggerFactory.getLogger(TicketDeliveryServiceImpl.class);
    
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
    
    @Value("${ticket.service.url:http://localhost:8083}")
    private String ticketServiceUrl;
    
    public TicketDeliveryServiceImpl(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository,
            EmailService emailService,
            RestTemplate restTemplate) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
    }
    
    @Override
    @Transactional
    public TicketDeliveryResponse deliverTickets(TicketDeliveryRequest request) {
        log.info("Delivering tickets for order: {} to user: {}", request.getOrderId(), request.getUserId());
        
        TicketDeliveryResponse response = new TicketDeliveryResponse();
        
        try {
            // Fetch ticket details from ticket service
            List<Map<String, Object>> ticketDetails = fetchTicketDetails(request.getTicketIds());
            
            if (ticketDetails.isEmpty()) {
                throw new IllegalArgumentException("No valid tickets found for delivery");
            }
            
            // Generate web links if requested
            List<String> webLinks = new ArrayList<>();
            if (request.isGenerateWebLink()) {
                for (UUID ticketId : request.getTicketIds()) {
                    webLinks.add(generateTicketWebLink(ticketId));
                }
                response.setTicketWebLinks(webLinks);
            }
            
            // Generate calendar event link if requested
            String calendarLink = null;
            if (request.isIncludeCalendarEvent() && !ticketDetails.isEmpty()) {
                Map<String, Object> firstTicket = ticketDetails.get(0);
                calendarLink = generateCalendarEventLink(
                    (String) firstTicket.get("eventName"),
                    (String) firstTicket.get("eventDate"),
                    (String) firstTicket.get("venueName"),
                    (String) firstTicket.get("venueAddress")
                );
                response.setCalendarEventLink(calendarLink);
            }
            
            // Deliver via specified channel
            Notification notification = null;
            if (request.getChannel() == NotificationChannel.EMAIL) {
                notification = deliverViaEmail(request, ticketDetails, webLinks, calendarLink);
            } else if (request.getChannel() == NotificationChannel.WEB) {
                notification = deliverViaWeb(request, ticketDetails, webLinks);
            } else if (request.getChannel() == NotificationChannel.MOBILE) {
                notification = deliverViaMobile(request, ticketDetails, webLinks);
            }
            
            if (notification != null) {
                response.setNotificationId(notification.getId());
                response.setDeliveryStatus(notification.getStatus().name());
                response.setMessage("Tickets delivered successfully");
            }
            
        } catch (Exception e) {
            log.error("Error delivering tickets: {}", e.getMessage(), e);
            response.setDeliveryStatus("FAILED");
            response.setMessage("Failed to deliver tickets: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public String generateTicketWebLink(UUID ticketId) {
        return baseUrl + "/tickets/" + ticketId.toString();
    }
    
    @Override
    public String generateCalendarEventLink(String eventName, String eventDate, String venueName, String venueAddress) {
        try {
            // Parse the event date
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime eventDateTime = LocalDateTime.parse(eventDate, formatter);
            
            // Format for iCal (YYYYMMDDTHHMMSS)
            String startDate = eventDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            String endDate = eventDateTime.plusHours(3).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            
            // Build Google Calendar link
            StringBuilder calendarUrl = new StringBuilder("https://calendar.google.com/calendar/render?action=TEMPLATE");
            calendarUrl.append("&text=").append(URLEncoder.encode(eventName, StandardCharsets.UTF_8));
            calendarUrl.append("&dates=").append(startDate).append("/").append(endDate);
            calendarUrl.append("&location=").append(URLEncoder.encode(venueName + ", " + venueAddress, StandardCharsets.UTF_8));
            calendarUrl.append("&details=").append(URLEncoder.encode("Event: " + eventName, StandardCharsets.UTF_8));
            
            return calendarUrl.toString();
        } catch (Exception e) {
            log.error("Error generating calendar link: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private List<Map<String, Object>> fetchTicketDetails(List<UUID> ticketIds) {
        List<Map<String, Object>> ticketDetails = new ArrayList<>();
        
        for (UUID ticketId : ticketIds) {
            try {
                String url = ticketServiceUrl + "/api/tickets/" + ticketId;
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                
                if (response != null && response.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ticketData = (Map<String, Object>) response.get("data");
                    ticketDetails.add(ticketData);
                }
            } catch (Exception e) {
                log.error("Error fetching ticket details for ticket {}: {}", ticketId, e.getMessage());
            }
        }
        
        return ticketDetails;
    }
    
    private Notification deliverViaEmail(
            TicketDeliveryRequest request,
            List<Map<String, Object>> ticketDetails,
            List<String> webLinks,
            String calendarLink) {
        
        log.info("Delivering tickets via email to: {}", request.getRecipientEmail());
        
        // Get or create ticket delivery template
        NotificationTemplate template = templateRepository.findByName("TICKET_DELIVERY")
                .orElseThrow(() -> new NotificationNotFoundException("Ticket delivery template not found"));
        
        // Build email content with ticket details
        String htmlContent = buildTicketEmailContent(ticketDetails, webLinks, calendarLink);
        String textContent = buildTicketTextContent(ticketDetails, webLinks, calendarLink);
        
        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTemplate(template);
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setSubject("Your Event Tickets - Order #" + request.getOrderId());
        notification.setHtmlContent(htmlContent);
        notification.setTextContent(textContent);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        
        notification = notificationRepository.save(notification);
        
        // Send email
        try {
            boolean sent = emailService.sendEmail(notification);
            
            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                log.info("Tickets delivered successfully via email");
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailedAt(Instant.now());
                notification.setErrorMessage("Email sending failed");
            }
            
        } catch (Exception e) {
            log.error("Failed to send ticket email: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedAt(Instant.now());
            notification.setErrorMessage(e.getMessage());
        }
        
        return notificationRepository.save(notification);
    }
    
    private Notification deliverViaWeb(
            TicketDeliveryRequest request,
            List<Map<String, Object>> ticketDetails,
            List<String> webLinks) {
        
        log.info("Delivering tickets via web portal to user: {}", request.getUserId());
        
        // Create notification record for web delivery
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setSubject("Tickets Available - Order #" + request.getOrderId());
        notification.setChannel(NotificationChannel.WEB);
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        notification.setRetryCount(0);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    private Notification deliverViaMobile(
            TicketDeliveryRequest request,
            List<Map<String, Object>> ticketDetails,
            List<String> webLinks) {
        
        log.info("Delivering tickets via mobile to user: {}", request.getUserId());
        
        // Create notification record for mobile delivery
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setSubject("Tickets Available - Order #" + request.getOrderId());
        notification.setChannel(NotificationChannel.MOBILE);
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        notification.setRetryCount(0);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    private String buildTicketEmailContent(
            List<Map<String, Object>> ticketDetails,
            List<String> webLinks,
            String calendarLink) {
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>Your Event Tickets</h2>");
        html.append("<p>Thank you for your purchase! Your tickets are ready.</p>");
        
        // Add ticket details
        for (int i = 0; i < ticketDetails.size(); i++) {
            Map<String, Object> ticket = ticketDetails.get(i);
            html.append("<div style='border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 5px;'>");
            html.append("<h3>").append(ticket.get("eventName")).append("</h3>");
            html.append("<p><strong>Date:</strong> ").append(ticket.get("eventDate")).append("</p>");
            html.append("<p><strong>Venue:</strong> ").append(ticket.get("venueName")).append("</p>");
            html.append("<p><strong>Ticket Number:</strong> ").append(ticket.get("ticketNumber")).append("</p>");
            html.append("<p><strong>Holder:</strong> ").append(ticket.get("holderName")).append("</p>");
            
            if (webLinks != null && i < webLinks.size()) {
                html.append("<p><a href='").append(webLinks.get(i))
                    .append("' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>")
                    .append("View Ticket</a></p>");
            }
            html.append("</div>");
        }
        
        // Add calendar link
        if (calendarLink != null) {
            html.append("<div style='margin: 20px 0;'>");
            html.append("<a href='").append(calendarLink)
                .append("' style='background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>")
                .append("Add to Calendar</a>");
            html.append("</div>");
        }
        
        html.append("<p style='margin-top: 30px; color: #666;'>")
            .append("Please present your ticket QR code at the venue entrance.</p>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String buildTicketTextContent(
            List<Map<String, Object>> ticketDetails,
            List<String> webLinks,
            String calendarLink) {
        
        StringBuilder text = new StringBuilder();
        text.append("Your Event Tickets\n\n");
        text.append("Thank you for your purchase! Your tickets are ready.\n\n");
        
        for (int i = 0; i < ticketDetails.size(); i++) {
            Map<String, Object> ticket = ticketDetails.get(i);
            text.append("Ticket ").append(i + 1).append(":\n");
            text.append("Event: ").append(ticket.get("eventName")).append("\n");
            text.append("Date: ").append(ticket.get("eventDate")).append("\n");
            text.append("Venue: ").append(ticket.get("venueName")).append("\n");
            text.append("Ticket Number: ").append(ticket.get("ticketNumber")).append("\n");
            text.append("Holder: ").append(ticket.get("holderName")).append("\n");
            
            if (webLinks != null && i < webLinks.size()) {
                text.append("View Ticket: ").append(webLinks.get(i)).append("\n");
            }
            text.append("\n");
        }
        
        if (calendarLink != null) {
            text.append("Add to Calendar: ").append(calendarLink).append("\n\n");
        }
        
        text.append("Please present your ticket QR code at the venue entrance.\n");
        
        return text.toString();
    }
}
