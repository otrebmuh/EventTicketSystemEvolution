package com.eventbooking.ticket.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.ticket.dto.GenerateTicketsRequest;
import com.eventbooking.ticket.dto.TicketDto;
import com.eventbooking.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<TicketDto>>> generateTickets(
            @Valid @RequestBody GenerateTicketsRequest request) {
        logger.info("Received request to generate tickets for order: {}", request.getOrderId());

        List<TicketDto> tickets = ticketService.generateTickets(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tickets generated successfully", tickets));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketDto>> getTicket(@PathVariable("ticketId") UUID ticketId) {
        logger.debug("Received request to get ticket: {}", ticketId);

        TicketDto ticket = ticketService.getTicketById(ticketId);

        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", ticket));
    }

    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<ApiResponse<TicketDto>> getTicketByNumber(@PathVariable String ticketNumber) {
        logger.debug("Received request to get ticket by number: {}", ticketNumber);

        TicketDto ticket = ticketService.getTicketByNumber(ticketNumber);

        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", ticket));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTicketsByOrder(@PathVariable("orderId") UUID orderId) {
        logger.debug("Received request to get tickets for order: {}", orderId);

        List<TicketDto> tickets = ticketService.getTicketsByOrderId(orderId);

        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTicketsByUser(@PathVariable("userId") UUID userId) {
        logger.debug("Received request to get tickets for user: {}", userId);

        List<TicketDto> tickets = ticketService.getTicketsByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelTicket(@PathVariable("ticketId") UUID ticketId) {
        logger.info("Received request to cancel ticket: {}", ticketId);

        ticketService.cancelTicket(ticketId);

        return ResponseEntity.ok(ApiResponse.success("Ticket cancelled successfully", "Ticket cancelled"));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TicketDto>> validateTicket(@RequestBody String qrCode) {
        logger.debug("Received request to validate ticket");

        TicketDto ticket = ticketService.validateTicket(qrCode);

        return ResponseEntity.ok(ApiResponse.success("Ticket validated successfully", ticket));
    }
}
