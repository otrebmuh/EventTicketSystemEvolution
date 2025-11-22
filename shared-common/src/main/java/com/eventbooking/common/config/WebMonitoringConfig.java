package com.eventbooking.common.config;

import com.eventbooking.common.interceptor.MetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for monitoring interceptors
 */
@Configuration
public class WebMonitoringConfig implements WebMvcConfigurer {

    private final MetricsInterceptor metricsInterceptor;

    public WebMonitoringConfig(MetricsInterceptor metricsInterceptor) {
        this.metricsInterceptor = metricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/actuator/**");
    }
}
