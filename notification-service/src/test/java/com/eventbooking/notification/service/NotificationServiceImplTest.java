package com.eventbooking.notification.service;

import com.eventbooking.notification.dto.NotificationDto;
import com.eventbooking.notification.dto.SendNotificationRequest;
import com.eventbooking.notification.entity.Notification;
import com.eventbooking.notification.entity.NotificationChannel;
import com.eventbooking.notification.entity.NotificationStatus;
import com.eventbooking.notification.entity.NotificationTemplate;
import com.eventbooking.notification.exception.NotificationNotFoundException;
import com.eventbooking.notification.mapper.NotificationMapper;
import com.eventbooking.notification.repository.NotificationRepository;
import com.eventbooking.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID notificationId;
    private UUID userId;
    private UUID templateId;
    private SendNotificationRequest sendRequest;
    private Notification notification;
    private NotificationDto notificationDto;
    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        Map<String, String> variables = new HashMap<>();
        variables.put("orderNumber", "ORD-12345");
        variables.put("userName", "John Doe");

        sendRequest = new SendNotificationRequest();
        sendRequest.setUserId(userId);
        sendRequest.setTemplateName("ORDER_CONFIRMATION");
        sendRequest.setRecipientEmail("user@example.com");
        sendRequest.setVariables(new HashMap<>(variables));

        template = new NotificationTemplate();
        template.setId(templateId);
        template.setName("ORDER_CONFIRMATION");
        template.setSubject("Order Confirmation - {{orderNumber}}");
        template.setHtmlContent("<html><body>Thank you {{userName}} for order {{orderNumber}}</body></html>");
        template.setTextContent("Thank you {{userName}} for order {{orderNumber}}");

        notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setTemplate(template);
        notification.setRecipientEmail("user@example.com");
        notification.setSubject("Order Confirmation - ORD-12345");
        notification.setHtmlContent("<html><body>Thank you John Doe for order ORD-12345</body></html>");
        notification.setTextContent("Thank you John Doe for order ORD-12345");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);

        notificationDto = new NotificationDto();
        notificationDto.setId(notificationId);
        notificationDto.setUserId(userId);
        notificationDto.setRecipientEmail("user@example.com");
        notificationDto.setSubject("Order Confirmation - ORD-12345");
        notificationDto.setStatus(NotificationStatus.SENT);
    }

    @Test
    void sendNotification_Success() {
        when(templateRepository.findByName("ORDER_CONFIRMATION")).thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(emailService.sendEmail(any(Notification.class))).thenReturn(true);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        NotificationDto result = notificationService.sendNotification(sendRequest);

        assertNotNull(result);
        assertEquals(notificationId, result.getId());
        assertEquals(NotificationStatus.SENT, result.getStatus());
        verify(templateRepository).findByName("ORDER_CONFIRMATION");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(any(Notification.class));
    }

    @Test
    void sendNotification_TemplateVariableReplacement() {
        when(templateRepository.findByName("ORDER_CONFIRMATION")).thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            assertTrue(saved.getSubject().contains("ORD-12345"));
            assertTrue(saved.getHtmlContent().contains("John Doe"));
            assertTrue(saved.getHtmlContent().contains("ORD-12345"));
            return saved;
        });
        when(emailService.sendEmail(any(Notification.class))).thenReturn(true);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        notificationService.sendNotification(sendRequest);

        verify(templateRepository).findByName("ORDER_CONFIRMATION");
    }

    @Test
    void sendNotification_EmailFailure() {
        when(templateRepository.findByName("ORDER_CONFIRMATION")).thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(emailService.sendEmail(any(Notification.class))).thenReturn(false);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        NotificationDto result = notificationService.sendNotification(sendRequest);

        assertNotNull(result);
        verify(emailService).sendEmail(any(Notification.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void getNotification_Success() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationMapper.toDto(notification)).thenReturn(notificationDto);

        NotificationDto result = notificationService.getNotification(notificationId);

        assertNotNull(result);
        assertEquals(notificationId, result.getId());
        verify(notificationRepository).findById(notificationId);
    }

    @Test
    void getNotification_ThrowsExceptionWhenNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, 
            () -> notificationService.getNotification(notificationId));
    }

    @Test
    void getUserNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(notification));
        
        when(notificationRepository.findByUserId(userId, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toDto(notification)).thenReturn(notificationDto);

        Page<NotificationDto> result = notificationService.getUserNotifications(userId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserId(userId, pageable);
    }

    @Test
    void getUserNotifications_WithStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(notification));
        
        when(notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT, pageable))
            .thenReturn(notificationPage);
        when(notificationMapper.toDto(notification)).thenReturn(notificationDto);

        Page<NotificationDto> result = notificationService.getUserNotifications(userId, NotificationStatus.SENT, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserIdAndStatus(userId, NotificationStatus.SENT, pageable);
    }

    @Test
    void resendNotification_Success() {
        notification.setStatus(NotificationStatus.FAILED);
        
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(emailService.retryEmail(notification)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        NotificationDto result = notificationService.resendNotification(notificationId);

        assertNotNull(result);
        verify(notificationRepository).findById(notificationId);
        verify(emailService).retryEmail(notification);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void processPendingNotifications_Success() {
        notification.setRetryCount(1);
        List<Notification> pendingNotifications = Arrays.asList(notification);
        
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.PENDING, 3))
            .thenReturn(pendingNotifications);
        when(emailService.retryEmail(notification)).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.processPendingNotifications();

        verify(notificationRepository).findByStatusAndRetryCountLessThan(NotificationStatus.PENDING, 3);
        verify(emailService).retryEmail(notification);
        verify(notificationRepository).save(any(Notification.class));
    }
}
