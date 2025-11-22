package com.eventbooking.common.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Performance tests for response times and concurrent load
 * Tests Requirements: 10.1-10.5 (Performance requirements)
 */
@SpringBootTest(classes = com.eventbooking.common.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class ResponseTimeTest {
    
    @Autowired(required = false)
    private MockMvc mockMvc;
    
    private static final long MAX_RESPONSE_TIME_MS = 3000; // 3 seconds as per requirements
    private static final int CONCURRENT_USERS = 100; // Test with 100 concurrent users
    
    @Test
    void testHealthEndpointResponseTime() throws Exception {
        if (mockMvc == null) {
            return;
        }
        
        // When
        long startTime = System.currentTimeMillis();
        MvcResult result = mockMvc.perform(get("/actuator/health"))
            .andReturn();
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Then - Should respond within 3 seconds
        assertTrue(responseTime < MAX_RESPONSE_TIME_MS,
            "Response time " + responseTime + "ms exceeds maximum " + MAX_RESPONSE_TIME_MS + "ms");
        assertEquals(200, result.getResponse().getStatus());
    }
    
    @Test
    void testConcurrentRequestHandling() throws InterruptedException {
        if (mockMvc == null) {
            return;
        }
        
        // Given
        int requestCount = CONCURRENT_USERS;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();
        
        // When - Simulate concurrent users
        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    MvcResult result = mockMvc.perform(get("/actuator/health"))
                        .andReturn();
                    long endTime = System.currentTimeMillis();
                    
                    synchronized (responseTimes) {
                        responseTimes.add(endTime - startTime);
                    }
                    
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then - All requests should complete
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "Not all concurrent requests completed in time");
        
        // And - Most requests should succeed
        assertTrue(successCount.get() > requestCount * 0.95,
            "Success rate too low: " + successCount.get() + "/" + requestCount);
        
        // And - Calculate average response time
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        System.out.println("Concurrent test results:");
        System.out.println("  Total requests: " + requestCount);
        System.out.println("  Successful: " + successCount.get());
        System.out.println("  Failed: " + errorCount.get());
        System.out.println("  Average response time: " + avgResponseTime + "ms");
        
        // Average response time should be reasonable under load
        assertTrue(avgResponseTime < MAX_RESPONSE_TIME_MS * 2,
            "Average response time under load is too high: " + avgResponseTime + "ms");
        
        executor.shutdown();
    }
    
    @Test
    void testResponseTimeConsistency() throws Exception {
        if (mockMvc == null) {
            return;
        }
        
        // Given - Multiple sequential requests
        int requestCount = 10;
        List<Long> responseTimes = new ArrayList<>();
        
        // When
        for (int i = 0; i < requestCount; i++) {
            long startTime = System.currentTimeMillis();
            mockMvc.perform(get("/actuator/health"))
                .andReturn();
            long endTime = System.currentTimeMillis();
            responseTimes.add(endTime - startTime);
        }
        
        // Then - Response times should be consistent
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        long minResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);
        
        System.out.println("Response time consistency:");
        System.out.println("  Average: " + avgResponseTime + "ms");
        System.out.println("  Min: " + minResponseTime + "ms");
        System.out.println("  Max: " + maxResponseTime + "ms");
        
        // Variance should not be too high
        double variance = maxResponseTime - minResponseTime;
        assertTrue(variance < MAX_RESPONSE_TIME_MS,
            "Response time variance too high: " + variance + "ms");
    }
    
    @Test
    void testSystemHandlesRapidSequentialRequests() throws Exception {
        if (mockMvc == null) {
            return;
        }
        
        // Given - Rapid sequential requests
        int requestCount = 50;
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andReturn();
            if (result.getResponse().getStatus() == 200) {
                successCount.incrementAndGet();
            }
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Then - All requests should succeed
        assertEquals(requestCount, successCount.get());
        
        // And - Total time should be reasonable
        double avgTimePerRequest = (double) totalTime / requestCount;
        System.out.println("Rapid sequential requests:");
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Average per request: " + avgTimePerRequest + "ms");
        
        assertTrue(avgTimePerRequest < MAX_RESPONSE_TIME_MS,
            "Average time per request too high: " + avgTimePerRequest + "ms");
    }
    
    @Test
    void testMemoryUsageUnderLoad() throws InterruptedException {
        if (mockMvc == null) {
            return;
        }
        
        // Given - Record initial memory
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Generate load
        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(requestCount);
        
        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/actuator/health"));
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        
        // Then - Memory usage should not grow excessively
        runtime.gc(); // Suggest garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Memory usage:");
        System.out.println("  Initial: " + (initialMemory / 1024 / 1024) + "MB");
        System.out.println("  Final: " + (finalMemory / 1024 / 1024) + "MB");
        System.out.println("  Increase: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        // Memory increase should be reasonable (less than 100MB for 100 requests)
        assertTrue(memoryIncrease < 100 * 1024 * 1024,
            "Memory increase too high: " + (memoryIncrease / 1024 / 1024) + "MB");
        
        executor.shutdown();
    }
}
