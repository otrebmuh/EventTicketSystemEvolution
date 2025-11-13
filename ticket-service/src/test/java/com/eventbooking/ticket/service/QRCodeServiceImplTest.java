package com.eventbooking.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeServiceImplTest {

    private QRCodeServiceImpl qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QRCodeServiceImpl();
    }

    // ========== QR Code Generation Tests ==========

    @Test
    void generateQRCode_WithValidInput_ShouldGenerateBase64QRCode() {
        String ticketId = "123e4567-e89b-12d3-a456-426614174000";
        String ticketNumber = "TKT-12345678-20240115120000-1234";

        String result = qrCodeService.generateQRCode(ticketId, ticketNumber);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Base64 encoded strings should not contain spaces or special characters except +, /, =
        assertTrue(result.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    void generateQRCode_WithDifferentInputs_ShouldGenerateDifferentQRCodes() {
        String ticketId1 = "123e4567-e89b-12d3-a456-426614174000";
        String ticketNumber1 = "TKT-12345678-20240115120000-1234";
        
        String ticketId2 = "987e6543-e21b-98d7-a654-426614174999";
        String ticketNumber2 = "TKT-87654321-20240115120000-5678";

        String qrCode1 = qrCodeService.generateQRCode(ticketId1, ticketNumber1);
        String qrCode2 = qrCodeService.generateQRCode(ticketId2, ticketNumber2);

        assertNotEquals(qrCode1, qrCode2);
    }

    @Test
    void generateQRCode_WithSameInputs_ShouldGenerateSameQRCode() {
        String ticketId = "123e4567-e89b-12d3-a456-426614174000";
        String ticketNumber = "TKT-12345678-20240115120000-1234";

        String qrCode1 = qrCodeService.generateQRCode(ticketId, ticketNumber);
        String qrCode2 = qrCodeService.generateQRCode(ticketId, ticketNumber);

        assertEquals(qrCode1, qrCode2);
    }

    // ========== QR Code Validation Tests ==========

    @Test
    void validateQRCode_WithValidFormat_ShouldReturnTrue() {
        String validQRCode = "TICKET:123e4567-e89b-12d3-a456-426614174000:TKT-12345678-20240115120000-1234";

        boolean result = qrCodeService.validateQRCode(validQRCode);

        assertTrue(result);
    }

    @Test
    void validateQRCode_WithInvalidFormat_ShouldReturnFalse() {
        String invalidQRCode = "INVALID:FORMAT";

        boolean result = qrCodeService.validateQRCode(invalidQRCode);

        assertFalse(result);
    }

    @Test
    void validateQRCode_WithNullInput_ShouldReturnFalse() {
        boolean result = qrCodeService.validateQRCode(null);

        assertFalse(result);
    }

    @Test
    void validateQRCode_WithEmptyString_ShouldReturnFalse() {
        boolean result = qrCodeService.validateQRCode("");

        assertFalse(result);
    }

    @Test
    void validateQRCode_WithoutTicketPrefix_ShouldReturnFalse() {
        String qrCodeWithoutPrefix = "123e4567-e89b-12d3-a456-426614174000:TKT-12345678";

        boolean result = qrCodeService.validateQRCode(qrCodeWithoutPrefix);

        assertFalse(result);
    }
}
