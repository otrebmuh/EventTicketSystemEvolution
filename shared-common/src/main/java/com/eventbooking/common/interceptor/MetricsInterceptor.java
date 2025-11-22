package com.eventbooking.common.interceptor;

import com.eventbooking.common.config.MonitoringConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor to automatically track API metrics and add tracing context
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MetricsInterceptor.class);
    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final String TRACE_ID = "traceId";

    private final MonitoringConfig.CustomMetrics customMetrics;

    public MetricsInterceptor(MonitoringConfig.CustomMetrics customMetrics) {
        this.customMetrics = customMetrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Record request start time
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

        // Add trace ID to MDC for logging
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);

        logger.debug("Request started: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        try {
            // Calculate request duration
            Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                String endpoint = request.getRequestURI();
                int statusCode = response.getStatus();

                // Record metrics
                customMetrics.recordApiCall(endpoint, statusCode, duration);

                // Log slow requests
                if (duration > 3000) {
                    logger.warn("Slow request detected: {} {} took {}ms (status: {})",
                        request.getMethod(), endpoint, duration, statusCode);
                }

                logger.debug("Request completed: {} {} in {}ms (status: {})",
                    request.getMethod(), endpoint, duration, statusCode);
            }
        } finally {
            // Clean up MDC
            MDC.remove(TRACE_ID);
        }
    }
}
