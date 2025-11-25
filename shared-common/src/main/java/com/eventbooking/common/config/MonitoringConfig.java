package com.eventbooking.common.config;

// X-Ray imports disabled for local development
// import com.amazonaws.xray.AWSXRay;
// import com.amazonaws.xray.AWSXRayRecorderBuilder;
// import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
// import com.amazonaws.xray.plugins.EC2Plugin;
// import com.amazonaws.xray.plugins.ECSPlugin;
// import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import jakarta.servlet.Filter;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * Configuration for application monitoring including CloudWatch metrics,
 * AWS X-Ray distributed tracing, and custom metrics.
 */
@Configuration
public class MonitoringConfig {

    @Value("${spring.application.name:event-booking-service}")
    private String applicationName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${monitoring.cloudwatch.enabled:true}")
    private boolean cloudWatchEnabled;

    @Value("${monitoring.xray.enabled:true}")
    private boolean xrayEnabled;

    @Value("${monitoring.cloudwatch.namespace:EventBookingSystem}")
    private String cloudWatchNamespace;

    /**
     * Configure CloudWatch metrics registry for publishing application metrics
     */
    @Bean
    @Profile("!test")
    public CloudWatchMeterRegistry cloudWatchMeterRegistry() {
        if (!cloudWatchEnabled) {
            return null;
        }

        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            private final Map<String, String> configuration = Map.of(
                    "cloudwatch.namespace", cloudWatchNamespace,
                    "cloudwatch.step", "PT1M" // 1 minute step
            );

            @Override
            public String get(String key) {
                return configuration.get(key);
            }

            @Override
            public Duration step() {
                return Duration.ofMinutes(1);
            }

            @Override
            public int batchSize() {
                return 20;
            }
        };

        CloudWatchAsyncClient cloudWatchAsyncClient = CloudWatchAsyncClient
                .builder()
                .region(Region.of(awsRegion))
                .build();

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
    }

    /**
     * Customize meter registry with common tags for all metrics
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", applicationName,
                        "environment", System.getProperty("spring.profiles.active", "default"));
    }

    /**
     * Configure AWS X-Ray for distributed tracing
     * DISABLED FOR LOCAL DEVELOPMENT - Uncomment when deploying to AWS
     */
    @Bean
    @Profile("!test")
    public Filter tracingFilter() {
        // X-Ray disabled for local development to avoid dependency conflicts
        // Re-enable this when deploying to AWS with proper X-Ray daemon
        return (request, response, chain) -> chain.doFilter(request, response);
    }

    /*
     * X-Ray configuration commented out for local development
     * 
     * @Bean
     * 
     * @Profile("!test")
     * public Filter tracingFilter() {
     * if (!xrayEnabled) {
     * return (request, response, chain) -> chain.doFilter(request, response);
     * }
     * 
     * // Configure X-Ray recorder
     * AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
     * .withPlugin(new EC2Plugin())
     * .withPlugin(new ECSPlugin());
     * 
     * // Load sampling rules from classpath if available
     * URL samplingRules =
     * getClass().getClassLoader().getResource("xray-sampling-rules.json");
     * if (samplingRules != null) {
     * builder.withSamplingStrategy(new LocalizedSamplingStrategy(samplingRules));
     * }
     * 
     * AWSXRay.setGlobalRecorder(builder.build());
     * 
     * return new AWSXRayServletFilter("EventBookingSystem");
     * }
     */

    /**
     * Custom metrics for business operations
     */
    @Bean
    public CustomMetrics customMetrics(MeterRegistry registry) {
        return new CustomMetrics(registry);
    }

    /**
     * Helper class for recording custom business metrics
     */
    public static class CustomMetrics {
        private final MeterRegistry registry;

        public CustomMetrics(MeterRegistry registry) {
            this.registry = registry;
        }

        public void recordUserRegistration() {
            registry.counter("user.registration.total").increment();
        }

        public void recordLogin(boolean success) {
            registry.counter("user.login.total", "success", String.valueOf(success)).increment();
        }

        public void recordEventCreation() {
            registry.counter("event.creation.total").increment();
        }

        public void recordTicketPurchase(double amount) {
            registry.counter("ticket.purchase.total").increment();
            registry.summary("ticket.purchase.amount").record(amount);
        }

        public void recordPaymentProcessing(boolean success, String paymentMethod) {
            registry.counter("payment.processing.total",
                    "success", String.valueOf(success),
                    "method", paymentMethod).increment();
        }

        public void recordNotificationSent(String type, boolean success) {
            registry.counter("notification.sent.total",
                    "type", type,
                    "success", String.valueOf(success)).increment();
        }

        public void recordApiCall(String endpoint, int statusCode, long durationMs) {
            registry.counter("api.calls.total",
                    "endpoint", endpoint,
                    "status", String.valueOf(statusCode)).increment();
            registry.timer("api.response.time",
                    "endpoint", endpoint).record(Duration.ofMillis(durationMs));
        }

        public void recordDatabaseQuery(String operation, long durationMs) {
            registry.timer("database.query.time",
                    "operation", operation).record(Duration.ofMillis(durationMs));
        }

        public void recordCacheHit(boolean hit) {
            registry.counter("cache.access.total",
                    "hit", String.valueOf(hit)).increment();
        }
    }
}
