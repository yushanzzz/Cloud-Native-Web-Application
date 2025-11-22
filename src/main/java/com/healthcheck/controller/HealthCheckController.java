package com.healthcheck.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.healthcheck.config.MetricsConfig;
import com.healthcheck.service.HealthCheckService;

import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
public class HealthCheckController {
    
    private final HealthCheckService healthCheckService;
    private final MetricsConfig metricsConfig;

    @Autowired
    public HealthCheckController(HealthCheckService healthCheckService, MetricsConfig metricsConfig) {
        this.healthCheckService = healthCheckService;
        this.metricsConfig = metricsConfig;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(HttpServletRequest request) {
        log.info("Demo GET /healthz - Health check request received");
        
        Counter counter = metricsConfig.getApiCounter("GET_healthz");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            // Check for query parameters
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                log.warn("GET /healthz - Request contains query parameters: {}", request.getQueryString());
                return createHealthCheckResponse(HttpStatus.BAD_REQUEST);
            }
            
            // Check for request body
            if (hasRequestBody(request)) {
                log.warn("GET /healthz - Request contains payload, Content-Length: {}", 
                         request.getHeader("Content-Length"));
                return createHealthCheckResponse(HttpStatus.BAD_REQUEST);
            }

            // Perform health check with database monitoring
            Timer.Sample dbSample = Timer.start();
            boolean isHealthy = healthCheckService.performHealthCheck();
            dbSample.stop(metricsConfig.getDatabaseTimer("health_check"));

            if (isHealthy) {
                log.info("GET /healthz - Health check successful");
                return createHealthCheckResponse(HttpStatus.OK);
            } else {
                log.error("GET /healthz - Health check failed - Database operation unsuccessful");
                return createHealthCheckResponse(HttpStatus.SERVICE_UNAVAILABLE);
            }

        } catch (Exception e) {
            log.error("GET /healthz - Health check error occurred", e);
            return createHealthCheckResponse(HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_healthz"));
        }
    }

    @RequestMapping(value = "/healthz", method = {
        RequestMethod.POST, 
        RequestMethod.PUT, 
        RequestMethod.DELETE, 
        RequestMethod.PATCH,
        RequestMethod.HEAD,
        RequestMethod.OPTIONS,
        RequestMethod.TRACE
    })
    public ResponseEntity<Void> healthCheckMethodNotAllowed(HttpServletRequest request) {
        log.warn("{} /healthz - Method not allowed", request.getMethod());
        
        Counter counter = metricsConfig.getApiCounter("UNSUPPORTED_healthz");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            return createHealthCheckResponse(HttpStatus.METHOD_NOT_ALLOWED);
        } finally {
            sample.stop(metricsConfig.getApiTimer("UNSUPPORTED_healthz"));
        }
    }

    @GetMapping("/")
    public ResponseEntity<String> root() {
        log.info("GET / - Root endpoint accessed");
        
        Counter counter = metricsConfig.getApiCounter("GET_root");
        Timer.Sample sample = Timer.start();
        counter.increment();
        
        try {
            String message = "Cloud-Native Health Check API. Use /healthz for health checks.";
            log.debug("GET / - Returning root message");
            return ResponseEntity.ok(message);
        } finally {
            sample.stop(metricsConfig.getApiTimer("GET_root"));
        }
    }

    private ResponseEntity<Void> createHealthCheckResponse(HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        
        return new ResponseEntity<>(headers, status);
    }

    private boolean hasRequestBody(HttpServletRequest request) {
        try {
            String contentLength = request.getHeader("Content-Length");
            if (contentLength != null && !contentLength.isEmpty()) {
                int length = Integer.parseInt(contentLength);
                if (length > 0) {
                    log.debug("Detected Content-Length: {}", length);
                    return true;
                }
            }

            String transferEncoding = request.getHeader("Transfer-Encoding");
            if ("chunked".equalsIgnoreCase(transferEncoding)) {
                log.debug("Detected chunked transfer encoding");
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Error checking request body, treating as no body", e);
            return false;
        }
    }
}