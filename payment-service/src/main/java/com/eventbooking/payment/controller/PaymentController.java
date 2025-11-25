package com.eventbooking.payment.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.common.saga.SagaContext;
import com.eventbooking.payment.dto.PaymentResponse;
import com.eventbooking.payment.dto.ProcessPaymentRequest;
import com.eventbooking.payment.dto.TicketPurchaseRequest;
import com.eventbooking.payment.dto.TicketPurchaseResponse;
import com.eventbooking.payment.saga.TicketPurchaseSaga;
import com.eventbooking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final TicketPurchaseSaga ticketPurchaseSaga;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        log.info("Processing payment request for order: {}", request.getOrderId());

        PaymentResponse response = paymentService.processPayment(request);

        if ("succeeded".equals(response.getStatus())) {
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
        } else if (response.getRequiresAction() != null && response.getRequiresAction()) {
            return ResponseEntity.ok(ApiResponse.success("Payment requires additional authentication", response));
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(response.getErrorMessage()));
        }
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable String paymentIntentId) {
        log.info("Confirming payment intent: {}", paymentIntentId);

        PaymentResponse response = paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", response));
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(value = "reason", required = false, defaultValue = "Customer requested refund") String reason) {
        log.info("Processing refund for order: {}", orderId);

        PaymentResponse response = paymentService.refundPayment(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable("transactionId") UUID transactionId) {
        log.debug("Fetching payment status for transaction: {}", transactionId);

        PaymentResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Purchase tickets using saga pattern for distributed transaction handling
     */
    @PostMapping("/purchase-tickets")
    public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseTickets(
            @Valid @RequestBody TicketPurchaseRequest request,
            @RequestHeader(value = "X-User-Id") UUID userId) {
        log.info("Processing ticket purchase for user: {}, event: {}", userId, request.getEventId());

        // Create saga context
        SagaContext context = TicketPurchaseSaga.createPurchaseContext();
        context.put("userId", userId);
        context.put("eventId", request.getEventId());
        context.put("ticketTypeId", request.getTicketTypeId());
        context.put("quantity", request.getQuantity());
        context.put("unitPrice", request.getUnitPrice());
        context.put("paymentMethodId", request.getPaymentMethodId());
        context.put("reservationId", request.getReservationId());

        // Execute saga
        boolean success = ticketPurchaseSaga.executePurchase(context);

        if (success) {
            UUID transactionId = context.get("transactionId", UUID.class);
            TicketPurchaseResponse response = TicketPurchaseResponse.builder()
                    .sagaId(context.getSagaId())
                    .orderId(context.get("orderId", UUID.class))
                    .orderNumber(context.get("orderNumber", String.class))
                    .transactionId(transactionId != null ? transactionId.toString() : null)
                    .status("SUCCESS")
                    .message("Ticket purchase completed successfully")
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Ticket purchase successful", response));
        } else {
            TicketPurchaseResponse response = TicketPurchaseResponse.builder()
                    .sagaId(context.getSagaId())
                    .status("FAILED")
                    .message(context.getErrorMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Ticket purchase failed: " + context.getErrorMessage()));
        }
    }
}
