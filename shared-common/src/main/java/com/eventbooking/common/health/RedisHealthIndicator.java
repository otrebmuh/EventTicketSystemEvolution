package com.eventbooking.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis connectivity and performance
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            RedisConnection connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            connection.close();
            
            long responseTime = System.currentTimeMillis() - startTime;

            Health.Builder healthBuilder = Health.up()
                .withDetail("cache", "Redis")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("status", "Connected")
                .withDetail("ping", pong);

            // Warn if response time is slow
            if (responseTime > 500) {
                return healthBuilder
                    .status("WARNING")
                    .withDetail("warning", "Redis response time is slow")
                    .build();
            }

            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("cache", "Redis")
                .build();
        }
    }
}
