package com.healthcheck.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthcheck.entity.HealthCheck;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    @Query("SELECT h FROM HealthCheck h ORDER BY h.checkDatetime DESC LIMIT :limit")
    List<HealthCheck> findRecentHealthChecks(int limit);

    @Query("SELECT COUNT(h) FROM HealthCheck h")
    long countAllHealthChecks();

    @Query(value = "SELECT 1", nativeQuery = true)
    Integer testConnection();
}