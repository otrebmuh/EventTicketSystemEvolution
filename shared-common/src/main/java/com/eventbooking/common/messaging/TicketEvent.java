package com.eventbooking.common.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TicketEvent {
    
    public enum EventType {
        TICKETS_GENERATED,
        TICKET_CANCELLED,
        TICKETS_DELIVERED,
        TICKET_DELIVERY_FAILED
    }
    
    private EventType eventType;
    private UUID orderId;
    private UUID userId;
    private UUID eventId;
    private List<UUID> ticketIds;
    private Integer quantity;
    private String holderName;
    private String holderEmail;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String errorMessage;
    
    public TicketEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public TicketEvent(EventType eventType, UUID orderId, UUID userId) {
        this();
        this.eventType = eventType;
        this.orderId = orderId;
        this.userId = userId;
    }

    // Getters and Setters
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public List<UUID> getTicketIds() {
        return ticketIds;
    }

    public void setTicketIds(List<UUID> ticketIds) {
        this.ticketIds = ticketIds;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getHolderEmail() {
        return holderEmail;
    }

    public void setHolderEmail(String holderEmail) {
        this.holderEmail = holderEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
