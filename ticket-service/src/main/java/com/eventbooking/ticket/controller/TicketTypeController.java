package com.eventbooking.ticket.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.ticket.dto.*;
import com.eventbooking.ticket.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeController {
    
    private final TicketTypeService ticketTypeService;
    
    @Autowired
    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<TicketTypeDto>> createTicketType(
            @Valid @RequestBody CreateTicketTypeRequest request,
            @RequestHeader("X-User-Id") UUID organizerId) {
        
        TicketTypeDto ticketType = ticketTypeService.createTicketType(request, organizerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Ticket type created successfully", ticketType));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketTypeDto>> getTicketTypeById(@PathVariable UUID id) {
        TicketTypeDto ticketType = ticketTypeService.getTicketTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(ticketType));
    }
    
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<TicketTypeDto>>> getTicketTypesByEventId(
            @PathVariable UUID eventId) {
        
        List<TicketTypeDto> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        return ResponseEntity.ok(ApiResponse.success(ticketTypes));
    }
    
    @GetMapping("/event/{eventId}/available")
    public ResponseEntity<ApiResponse<List<TicketTypeDto>>> getAvailableTicketTypesByEventId(
            @PathVariable UUID eventId) {
        
        List<TicketTypeDto> ticketTypes = ticketTypeService.getAvailableTicketTypesByEventId(eventId);
        return ResponseEntity.ok(ApiResponse.success(ticketTypes));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketTypeDto>> updateTicketType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketTypeRequest request,
            @RequestHeader("X-User-Id") UUID organizerId) {
        
        TicketTypeDto ticketType = ticketTypeService.updateTicketType(id, request, organizerId);
        return ResponseEntity.ok(ApiResponse.success("Ticket type updated successfully", ticketType));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicketType(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID organizerId) {
        
        ticketTypeService.deleteTicketType(id, organizerId);
        return ResponseEntity.ok(ApiResponse.success("Ticket type deleted successfully", null));
    }
}
