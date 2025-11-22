package com.healthcheck.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private final DataSource dataSource;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

  
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Starting database configuration validation...");
        try {
            validateDatabaseConnection();   
            logEssentialDatabaseInfo();     // 印出 DB 資訊
            logger.info("Database configuration validation completed.");
        } catch (Exception e) {
            logger.error("Database configuration validation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database configuration validation failed", e);
        }
    }

    private void validateDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new RuntimeException("Database connection invalid");
            }
            logger.info("Database connection validation successful");
        }
    }

    //輸出資料庫名稱、版本、DDL 模式、表是否存在
    private void logEssentialDatabaseInfo() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            logger.info("Database: {} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            logger.info("DDL Mode: {}", ddlAuto);

            boolean tableExists;
            try (ResultSet rs = metaData.getTables(null, null, "health_checks", new String[] { "TABLE" })) {
                tableExists = rs.next();
            }
            logger.info("health_checks table exists: {}", tableExists);
        }
    }
}
