package com.eventbooking.ticket.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class QRCodeServiceImpl implements QRCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(QRCodeServiceImpl.class);
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    
    @Override
    public String generateQRCode(String ticketId, String ticketNumber) {
        try {
            // Create QR code content with ticket information
            String qrContent = String.format("TICKET:%s:%s", ticketId, ticketNumber);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 
                    QR_CODE_WIDTH, QR_CODE_HEIGHT);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            String base64QRCode = Base64.getEncoder().encodeToString(qrCodeBytes);
            
            logger.info("Generated QR code for ticket: {}", ticketNumber);
            return base64QRCode;
            
        } catch (WriterException | IOException e) {
            logger.error("Error generating QR code for ticket: {}", ticketNumber, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    @Override
    public boolean validateQRCode(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isEmpty()) {
            return false;
        }
        
        // Basic validation - check if it starts with expected prefix
        return qrCodeData.startsWith("TICKET:");
    }
}
