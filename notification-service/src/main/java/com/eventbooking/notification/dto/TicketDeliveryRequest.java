package com.eventbooking.notification.dto;

import com.eventbooking.notification.entity.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class TicketDeliveryRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotEmpty(message = "At least one ticket ID is required")
    private List<UUID> ticketIds;
    
    @NotNull(message = "Delivery channel is required")
    private NotificationChannel channel;
    
    private boolean includeCalendarEvent;
    
    private boolean generateWebLink;
    
    public TicketDeliveryRequest() {
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public List<UUID> getTicketIds() {
        return ticketIds;
    }
    
    public void setTicketIds(List<UUID> ticketIds) {
        this.ticketIds = ticketIds;
    }
    
    public NotificationChannel getChannel() {
        return channel;
    }
    
    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }
    
    public boolean isIncludeCalendarEvent() {
        return includeCalendarEvent;
    }
    
    public void setIncludeCalendarEvent(boolean includeCalendarEvent) {
        this.includeCalendarEvent = includeCalendarEvent;
    }
    
    public boolean isGenerateWebLink() {
        return generateWebLink;
    }
    
    public void setGenerateWebLink(boolean generateWebLink) {
        this.generateWebLink = generateWebLink;
    }
}
