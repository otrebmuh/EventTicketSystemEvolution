package com.eventbooking.notification.service;

import com.eventbooking.notification.entity.Notification;
import com.eventbooking.notification.entity.NotificationChannel;
import com.eventbooking.notification.entity.NotificationStatus;
import com.eventbooking.notification.entity.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Notification notification;
    private NotificationTemplate template;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        template = new NotificationTemplate();
        template.setId(UUID.randomUUID());
        template.setName("ORDER_CONFIRMATION");

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(UUID.randomUUID());
        notification.setTemplate(template);
        notification.setRecipientEmail("user@example.com");
        notification.setSubject("Order Confirmation");
        notification.setHtmlContent("<html><body>Thank you for your order</body></html>");
        notification.setTextContent("Thank you for your order");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);

        mimeMessage = mock(MimeMessage.class);
    }

    @Test
    void sendEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendEmail(notification);

        assertTrue(result);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_Failure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendEmail(notification);

        assertFalse(result);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_WithNullRecipient() {
        notification.setRecipientEmail(null);

        boolean result = emailService.sendEmail(notification);

        assertFalse(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_WithEmptyRecipient() {
        notification.setRecipientEmail("");

        boolean result = emailService.sendEmail(notification);

        assertFalse(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void retryEmail_Success() {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.retryEmail(notification);

        assertTrue(result);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void retryEmail_Failure() {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.retryEmail(notification);

        assertFalse(result);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void retryEmail_MaxRetriesExceeded() {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(3);

        boolean result = emailService.retryEmail(notification);

        assertFalse(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_HandlesMailException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(mock(MailException.class)).when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendEmail(notification);

        assertFalse(result);
        verify(mailSender).send(any(MimeMessage.class));
    }
}
