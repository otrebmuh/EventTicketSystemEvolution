package com.eventbooking.payment.config;

import com.eventbooking.common.saga.InMemorySagaEventStore;
import com.eventbooking.common.saga.SagaEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Saga pattern components
 */
@Configuration
public class SagaConfig {
    
    /**
     * Create saga event store bean
     * In production, this would be replaced with a persistent implementation
     */
    @Bean
    public SagaEventStore sagaEventStore() {
        return new InMemorySagaEventStore();
    }
}
