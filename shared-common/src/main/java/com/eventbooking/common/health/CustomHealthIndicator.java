package com.eventbooking.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Custom health indicator for monitoring application health metrics
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    private static final double MEMORY_THRESHOLD = 0.9; // 90% memory usage threshold

    @Override
    public Health health() {
        try {
            // Check memory usage
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            long maxMemory = heapUsage.getMax();
            long usedMemory = heapUsage.getUsed();
            double memoryUsageRatio = (double) usedMemory / maxMemory;

            Health.Builder healthBuilder = Health.up()
                .withDetail("memory.used", formatBytes(usedMemory))
                .withDetail("memory.max", formatBytes(maxMemory))
                .withDetail("memory.usage.percent", String.format("%.2f%%", memoryUsageRatio * 100));

            // Check if memory usage is critical
            if (memoryUsageRatio > MEMORY_THRESHOLD) {
                return healthBuilder
                    .status("WARNING")
                    .withDetail("warning", "Memory usage is above " + (MEMORY_THRESHOLD * 100) + "%")
                    .build();
            }

            return healthBuilder.build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
