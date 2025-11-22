package com.healthcheck.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthcheck.entity.HealthCheck;
import com.healthcheck.repository.HealthCheckRepository;

@Service
@Transactional
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private final HealthCheckRepository healthCheckRepository;

    @Autowired
    public HealthCheckService(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
    }

    public boolean performHealthCheck() {
    try {
        logger.debug("Starting health check execution...");

        Integer connectionTest = healthCheckRepository.testConnection();
        if (connectionTest == null || connectionTest != 1) {
            logger.error("Database connection test failed");
            return false;
        }

   
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.setCheckDatetime(Instant.now()); // Instant.now() 總是 UTC
        
        HealthCheck savedHealthCheck = healthCheckRepository.save(healthCheck);

        logger.info("Health check successful, record ID: {}", 
                   savedHealthCheck.getCheckId());

        return true;

    } catch (Exception e) {
        logger.error("Unexpected error occurred during health check execution: {}", e.getMessage(), e);
        return false;
    }
}

    @Transactional(readOnly = true)
    public boolean testDatabaseConnection() {
        try {
            Integer result = healthCheckRepository.testConnection();
            return result != null && result == 1;
        } catch (Exception e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public long getHealthCheckCount() {
        try {
            return healthCheckRepository.countAllHealthChecks();
        } catch (Exception e) {
            logger.error("Failed to get health check record count: {}", e.getMessage());
            return -1;
        }
    }
}