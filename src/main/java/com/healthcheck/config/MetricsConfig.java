package com.healthcheck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;

@Component
@Configuration
public class MetricsConfig {
    
    private final MeterRegistry meterRegistry;
    
    public MetricsConfig(@Lazy MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public Counter getApiCounter(String endpoint) {
        return Counter.builder("api.calls")
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }
    
    public Timer getApiTimer(String endpoint) {
        return Timer.builder("api.response.time")
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }
    
    public Timer getDatabaseTimer(String operation) {
        return Timer.builder("database.query.time")
                .tag("operation", operation)
                .register(meterRegistry);
    }
    
    public Timer getS3Timer(String operation) {
        return Timer.builder("s3.operation.time")
                .tag("operation", operation)
                .register(meterRegistry);
    }

    @Bean
    @Lazy  // 延遲初始化
    public MeterFilter onlyCustomMetrics() {
        return MeterFilter.accept(id -> {
            String name = id.getName();
            return name.equals("api.calls") || 
                   name.equals("api.response.time") ||
                   name.equals("database.query.time") ||
                   name.equals("s3.operation.time");
        });
    }
}