package com.eventbooking.ticket.service;

public interface QRCodeService {
    
    /**
     * Generate a QR code string for a ticket
     * @param ticketId The unique ticket identifier
     * @param ticketNumber The ticket number
     * @return Base64 encoded QR code image
     */
    String generateQRCode(String ticketId, String ticketNumber);
    
    /**
     * Validate a QR code
     * @param qrCodeData The QR code data to validate
     * @return true if valid, false otherwise
     */
    boolean validateQRCode(String qrCodeData);
}
