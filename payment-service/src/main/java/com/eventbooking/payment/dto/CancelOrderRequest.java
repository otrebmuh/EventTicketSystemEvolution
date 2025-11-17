package com.eventbooking.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private String cancellationReason;
    
    // For partial cancellation - list of order item IDs to cancel
    private List<UUID> orderItemIds;
}
