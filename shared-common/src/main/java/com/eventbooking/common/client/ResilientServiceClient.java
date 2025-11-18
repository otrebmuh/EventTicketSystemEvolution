package com.eventbooking.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

public abstract class ResilientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientServiceClient.class);

    protected final RestTemplate restTemplate;
    protected final CircuitBreaker circuitBreaker;
    protected final Retry retry;

    protected ResilientServiceClient(RestTemplate restTemplate,
                                    CircuitBreakerRegistry circuitBreakerRegistry,
                                    RetryRegistry retryRegistry,
                                    String serviceName) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        this.retry = retryRegistry.retry(serviceName);

        // Add event listeners for monitoring
        setupCircuitBreakerListeners();
        setupRetryListeners();
    }

    protected <T> T executeWithResilience(Supplier<T> supplier) {
        // Wrap with circuit breaker
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        
        // Wrap with retry
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        
        return decoratedSupplier.get();
    }

    protected <T> T executeWithResilienceAndFallback(Supplier<T> supplier, T fallbackValue) {
        try {
            return executeWithResilience(supplier);
        } catch (Exception e) {
            log.error("All resilience mechanisms exhausted, returning fallback", e);
            return fallbackValue;
        }
    }

    private void setupCircuitBreakerListeners() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> 
                    log.warn("Circuit breaker recorded error: {}", 
                        event.getThrowable().getMessage()))
                .onSuccess(event -> 
                    log.debug("Circuit breaker recorded success"));
    }

    private void setupRetryListeners() {
        retry.getEventPublisher()
                .onRetry(event -> 
                    log.warn("Retry attempt {} for operation", 
                        event.getNumberOfRetryAttempts()))
                .onSuccess(event -> 
                    log.info("Operation succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()))
                .onError(event -> 
                    log.error("Operation failed after {} attempts", 
                        event.getNumberOfRetryAttempts()));
    }
}
