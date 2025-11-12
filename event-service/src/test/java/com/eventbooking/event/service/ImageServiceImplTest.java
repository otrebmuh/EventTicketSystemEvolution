package com.eventbooking.event.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.eventbooking.event.exception.InvalidEventDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private ImageServiceImpl imageService;

    private UUID eventId;
    private String bucketName;
    private String cloudFrontDomain;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        bucketName = "test-bucket";
        cloudFrontDomain = "d123456.cloudfront.net";
        
        ReflectionTestUtils.setField(imageService, "bucketName", bucketName);
        ReflectionTestUtils.setField(imageService, "cloudFrontDomain", cloudFrontDomain);
    }

    // ========== Image Upload Tests ==========

    @Test
    void uploadEventImage_WithValidJpegImage_ShouldUploadSuccessfully() throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
            "image",
            "test-image.jpg",
            "image/jpeg",
            imageBytes
        );

        String expectedUrl = String.format("https://%s.s3.amazonaws.com/events/%s/test.jpg", 
            bucketName, eventId);
        
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(null);
        when(amazonS3.getUrl(eq(bucketName), anyString())).thenReturn(new URL(expectedUrl));

        String result = imageService.uploadEventImage(file, eventId);

        assertNotNull(result);
        assertTrue(result.contains(bucketName));
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    @Test
    void uploadEventImage_WithValidPngImage_ShouldUploadSuccessfully() throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
            "image",
            "test-image.png",
            "image/png",
            imageBytes
        );

        String expectedUrl = String.format("https://%s.s3.amazonaws.com/events/%s/test.png", 
            bucketName, eventId);
        
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(null);
        when(amazonS3.getUrl(eq(bucketName), anyString())).thenReturn(new URL(expectedUrl));

        String result = imageService.uploadEventImage(file, eventId);

        assertNotNull(result);
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    @Test
    void uploadEventImage_WithNullFile_ShouldThrowException() {
        assertThrows(InvalidEventDataException.class, () -> 
            imageService.uploadEventImage(null, eventId)
        );
    }

    @Test
    void uploadEventImage_WithEmptyFile_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            new byte[0]
        );

        assertThrows(InvalidEventDataException.class, () -> 
            imageService.uploadEventImage(file, eventId)
        );
    }

    @Test
    void uploadEventImage_WithInvalidContentType_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
            "image",
            "test.txt",
            "text/plain",
            "not an image".getBytes()
        );

        assertThrows(InvalidEventDataException.class, () -> 
            imageService.uploadEventImage(file, eventId)
        );
    }

    @Test
    void uploadEventImage_WithOversizedFile_ShouldThrowException() {
        byte[] largeFile = new byte[11 * 1024 * 1024]; // 11MB (exceeds 10MB limit)
        
        MockMultipartFile file = new MockMultipartFile(
            "image",
            "large-image.jpg",
            "image/jpeg",
            largeFile
        );

        assertThrows(InvalidEventDataException.class, () -> 
            imageService.uploadEventImage(file, eventId)
        );
    }

    @Test
    void uploadEventImage_WithLargeImage_ShouldResizeImage() throws IOException {
        // Create a large image (2400x1800)
        BufferedImage largeImage = new BufferedImage(2400, 1800, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(largeImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
            "image",
            "large-image.jpg",
            "image/jpeg",
            imageBytes
        );

        String expectedUrl = String.format("https://%s.s3.amazonaws.com/events/%s/test.jpg", 
            bucketName, eventId);
        
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(null);
        when(amazonS3.getUrl(eq(bucketName), anyString())).thenReturn(new URL(expectedUrl));

        String result = imageService.uploadEventImage(file, eventId);

        assertNotNull(result);
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    // ========== Image Deletion Tests ==========

    @Test
    void deleteEventImage_WithValidUrl_ShouldDeleteFromS3() {
        String imageUrl = String.format("https://%s.s3.amazonaws.com/events/%s/image.jpg", 
            bucketName, eventId);

        doNothing().when(amazonS3).deleteObject(any(DeleteObjectRequest.class));

        imageService.deleteEventImage(imageUrl);

        verify(amazonS3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteEventImage_WithNullUrl_ShouldNotCallS3() {
        imageService.deleteEventImage(null);

        verify(amazonS3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteEventImage_WithEmptyUrl_ShouldNotCallS3() {
        imageService.deleteEventImage("");

        verify(amazonS3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteEventImage_WithS3Exception_ShouldNotThrowException() {
        String imageUrl = String.format("https://%s.s3.amazonaws.com/events/%s/image.jpg", 
            bucketName, eventId);

        doThrow(new RuntimeException("S3 error")).when(amazonS3)
            .deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> imageService.deleteEventImage(imageUrl));
    }

    // ========== CDN URL Tests ==========

    @Test
    void getCdnUrl_WithCloudFrontConfigured_ShouldReturnCdnUrl() {
        String s3Url = String.format("https://%s.s3.amazonaws.com/events/%s/image.jpg", 
            bucketName, eventId);
        
        String result = imageService.getCdnUrl(s3Url);

        assertNotNull(result);
        assertTrue(result.contains(cloudFrontDomain));
        assertTrue(result.startsWith("https://"));
    }

    @Test
    void getCdnUrl_WithoutCloudFrontConfigured_ShouldReturnS3Url() {
        ReflectionTestUtils.setField(imageService, "cloudFrontDomain", "");
        
        String s3Url = String.format("https://%s.s3.amazonaws.com/events/%s/image.jpg", 
            bucketName, eventId);
        
        String result = imageService.getCdnUrl(s3Url);

        assertEquals(s3Url, result);
    }

    @Test
    void getCdnUrl_WithNullCloudFrontDomain_ShouldReturnS3Url() {
        ReflectionTestUtils.setField(imageService, "cloudFrontDomain", null);
        
        String s3Url = String.format("https://%s.s3.amazonaws.com/events/%s/image.jpg", 
            bucketName, eventId);
        
        String result = imageService.getCdnUrl(s3Url);

        assertEquals(s3Url, result);
    }
}
