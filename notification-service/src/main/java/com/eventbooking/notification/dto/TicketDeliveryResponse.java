package com.eventbooking.notification.dto;

import java.util.List;
import java.util.UUID;

public class TicketDeliveryResponse {
    
    private UUID notificationId;
    private String deliveryStatus;
    private List<String> ticketWebLinks;
    private String calendarEventLink;
    private String message;
    
    public TicketDeliveryResponse() {
    }
    
    public UUID getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public List<String> getTicketWebLinks() {
        return ticketWebLinks;
    }
    
    public void setTicketWebLinks(List<String> ticketWebLinks) {
        this.ticketWebLinks = ticketWebLinks;
    }
    
    public String getCalendarEventLink() {
        return calendarEventLink;
    }
    
    public void setCalendarEventLink(String calendarEventLink) {
        this.calendarEventLink = calendarEventLink;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
