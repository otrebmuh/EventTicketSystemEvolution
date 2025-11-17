package com.eventbooking.notification.service;

import com.eventbooking.notification.dto.CreateTemplateRequest;
import com.eventbooking.notification.dto.TemplateDto;
import com.eventbooking.notification.entity.NotificationTemplate;
import com.eventbooking.notification.entity.TemplateCategory;
import com.eventbooking.notification.exception.TemplateAlreadyExistsException;
import com.eventbooking.notification.exception.TemplateNotFoundException;
import com.eventbooking.notification.mapper.TemplateMapper;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private UUID templateId;
    private UUID createdBy;
    private CreateTemplateRequest createRequest;
    private NotificationTemplate template;
    private TemplateDto templateDto;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        createdBy = UUID.randomUUID();

        createRequest = new CreateTemplateRequest();
        createRequest.setName("ORDER_CONFIRMATION");
        createRequest.setSubject("Order Confirmation - {{orderNumber}}");
        createRequest.setHtmlContent("<html><body>Thank you for your order {{orderNumber}}</body></html>");
        createRequest.setTextContent("Thank you for your order {{orderNumber}}");
        createRequest.setCategory(TemplateCategory.TRANSACTIONAL);

        template = new NotificationTemplate();
        template.setId(templateId);
        template.setName("ORDER_CONFIRMATION");
        template.setSubject("Order Confirmation - {{orderNumber}}");
        template.setHtmlContent("<html><body>Thank you for your order {{orderNumber}}</body></html>");
        template.setTextContent("Thank you for your order {{orderNumber}}");
        template.setCategory(TemplateCategory.TRANSACTIONAL);
        template.setIsActive(true);

        templateDto = new TemplateDto();
        templateDto.setId(templateId);
        templateDto.setName("ORDER_CONFIRMATION");
        templateDto.setSubject("Order Confirmation - {{orderNumber}}");
        templateDto.setCategory(TemplateCategory.TRANSACTIONAL);
    }

    @Test
    void createTemplate_Success() {
        when(templateRepository.existsByName(createRequest.getName())).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        TemplateDto result = templateService.createTemplate(createRequest, createdBy);

        assertNotNull(result);
        assertEquals("ORDER_CONFIRMATION", result.getName());
        verify(templateRepository).existsByName(createRequest.getName());
        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    @Test
    void createTemplate_ThrowsExceptionWhenNameExists() {
        when(templateRepository.existsByName(createRequest.getName())).thenReturn(true);

        assertThrows(TemplateAlreadyExistsException.class, 
            () -> templateService.createTemplate(createRequest, createdBy));
        
        verify(templateRepository).existsByName(createRequest.getName());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void getTemplate_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        TemplateDto result = templateService.getTemplate(templateId);

        assertNotNull(result);
        assertEquals(templateId, result.getId());
        verify(templateRepository).findById(templateId);
    }

    @Test
    void getTemplate_ThrowsExceptionWhenNotFound() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, 
            () -> templateService.getTemplate(templateId));
    }

    @Test
    void getTemplateByName_Success() {
        when(templateRepository.findByName("ORDER_CONFIRMATION")).thenReturn(Optional.of(template));
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        TemplateDto result = templateService.getTemplateByName("ORDER_CONFIRMATION");

        assertNotNull(result);
        assertEquals("ORDER_CONFIRMATION", result.getName());
        verify(templateRepository).findByName("ORDER_CONFIRMATION");
    }

    @Test
    void updateTemplate_Success() {
        CreateTemplateRequest updateRequest = new CreateTemplateRequest();
        updateRequest.setName("ORDER_CONFIRMATION");
        updateRequest.setSubject("Updated Subject");
        updateRequest.setHtmlContent("<html>Updated</html>");
        updateRequest.setTextContent("Updated");
        updateRequest.setCategory(TemplateCategory.TRANSACTIONAL);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        TemplateDto result = templateService.updateTemplate(templateId, updateRequest);

        assertNotNull(result);
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    @Test
    void listTemplates_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<NotificationTemplate> templatePage = new PageImpl<>(Arrays.asList(template));
        
        when(templateRepository.findAll(pageable)).thenReturn(templatePage);
        when(templateMapper.toDto(template)).thenReturn(templateDto);

        Page<TemplateDto> result = templateService.listTemplates(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(templateRepository).findAll(pageable);
    }

    @Test
    void setTemplateActive_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        templateService.setTemplateActive(templateId, false);

        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    @Test
    void deleteTemplate_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        doNothing().when(templateRepository).delete(template);

        templateService.deleteTemplate(templateId);

        verify(templateRepository).findById(templateId);
        verify(templateRepository).delete(template);
    }
}
