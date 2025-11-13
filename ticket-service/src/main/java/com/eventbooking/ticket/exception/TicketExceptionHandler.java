package com.eventbooking.ticket.exception;

import com.eventbooking.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class TicketExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketExceptionHandler.class);
    
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTicketNotFoundException(
            TicketNotFoundException ex) {
        
        String requestId = UUID.randomUUID().toString();
        logger.error("Ticket not found - RequestId: {}, Error: {}", requestId, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(TicketTypeNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTicketTypeNotFoundException(
            TicketTypeNotFoundException ex) {
        
        String requestId = UUID.randomUUID().toString();
        logger.error("Ticket type not found - RequestId: {}, Error: {}", requestId, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientInventoryException(
            InsufficientInventoryException ex) {
        
        String requestId = UUID.randomUUID().toString();
        logger.error("Insufficient inventory - RequestId: {}, Error: {}", requestId, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(InvalidReservationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidReservationException(
            InvalidReservationException ex) {
        
        String requestId = UUID.randomUUID().toString();
        logger.error("Invalid reservation - RequestId: {}, Error: {}", requestId, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
