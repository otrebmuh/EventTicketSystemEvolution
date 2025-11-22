package com.eventbooking.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Health indicator for database connectivity and performance
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                if (resultSet.next()) {
                    Health.Builder healthBuilder = Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Connected");

                    // Warn if response time is slow
                    if (responseTime > 1000) {
                        return healthBuilder
                            .status("WARNING")
                            .withDetail("warning", "Database response time is slow")
                            .build();
                    }

                    return healthBuilder.build();
                }
            }
            
            return Health.down()
                .withDetail("error", "Unable to execute test query")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("database", "PostgreSQL")
                .build();
        }
    }
}
