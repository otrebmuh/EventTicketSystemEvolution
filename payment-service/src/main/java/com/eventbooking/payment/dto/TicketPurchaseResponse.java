package com.eventbooking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for ticket purchase saga execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPurchaseResponse {
    
    private UUID sagaId;
    private UUID orderId;
    private String orderNumber;
    private String transactionId;
    private String status;
    private String message;
}
