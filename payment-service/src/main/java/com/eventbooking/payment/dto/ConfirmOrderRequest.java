package com.eventbooking.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private String paymentIntentId;
}
