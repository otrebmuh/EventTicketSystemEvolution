package com.eventbooking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    
    private UUID refundId;
    private UUID orderId;
    private String orderNumber;
    private BigDecimal refundAmount;
    private String refundStatus;
    private String gatewayRefundId;
    private Instant refundedAt;
    private String message;
}
