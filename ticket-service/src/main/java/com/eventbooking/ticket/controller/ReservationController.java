package com.eventbooking.ticket.controller;

import com.eventbooking.common.dto.ApiResponse;
import com.eventbooking.ticket.dto.ReservationDto;
import com.eventbooking.ticket.dto.ReserveTicketsRequest;
import com.eventbooking.ticket.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    
    private final TicketTypeService ticketTypeService;
    
    @Autowired
    public ReservationController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDto>> reserveTickets(
            @Valid @RequestBody ReserveTicketsRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        ReservationDto reservation = ticketTypeService.reserveTickets(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tickets reserved successfully", reservation));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        
        ticketTypeService.cancelReservation(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully", null));
    }
    
    @GetMapping("/my-reservations")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getMyActiveReservations(
            @RequestHeader("X-User-Id") UUID userId) {
        
        List<ReservationDto> reservations = ticketTypeService.getUserActiveReservations(userId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }
}
