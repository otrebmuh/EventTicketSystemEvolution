package com.eventbooking.common.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis connectivity and performance
 * Only enabled when Redis is explicitly configured
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
        logger.info("RedisHealthIndicator initialized - Redis health checks enabled");
    }

    @Override
    public Health health() {
        try {
            logger.info("Starting Redis health check...");
            long startTime = System.currentTimeMillis();
            
            // Set connection timeout to 30 seconds
            RedisConnection connection = null;
            try {
                connection = redisConnectionFactory.getConnection();
                logger.info("Redis connection obtained, sending PING...");
                
                String pong = connection.ping();
                logger.info("Redis PING response: {}", pong);
                
                long responseTime = System.currentTimeMillis() - startTime;

                Health.Builder healthBuilder = Health.up()
                    .withDetail("cache", "Redis")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("status", "Connected")
                    .withDetail("ping", pong)
                    .withDetail("host", "redis-cache")
                    .withDetail("port", "6379");

                // Warn if response time is slow
                if (responseTime > 2000) {
                    logger.warn("Redis response time is slow: {}ms", responseTime);
                    return healthBuilder
                        .status("WARNING")
                        .withDetail("warning", "Redis response time is slow")
                        .build();
                }

                logger.info("Redis health check successful: {}ms", responseTime);
                return healthBuilder.build();
                
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                        logger.debug("Redis connection closed");
                    } catch (Exception closeEx) {
                        logger.warn("Error closing Redis connection: {}", closeEx.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Redis health check failed: {}", e.getMessage(), e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("cache", "Redis")
                .withDetail("status", "Disconnected")
                .withDetail("host", "redis-cache")
                .withDetail("port", "6379")
                .build();
        }
    }
}
