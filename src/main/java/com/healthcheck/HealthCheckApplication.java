package com.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //啟用事務管理
public class HealthCheckApplication{

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckApplication.class);

    public static void main(String[] args){
  
        
        
    try {
        logger.info("Starting Health Check API...");

        System.setProperty("spring.application.name", "health-check-api");

        SpringApplication app = new SpringApplication(HealthCheckApplication.class);
        app.run(args);

        logger.info("Health Check API started successfully");
    } catch (Exception e) {
        logger.error("Application startup failed: {}", e.getMessage(), e);
        System.exit(1);
    }
}

    static{
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Health Check API shutting down...");
            logger.info("Application closed successfully");
        }));
    }    
}