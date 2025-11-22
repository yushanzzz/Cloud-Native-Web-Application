package com.healthcheck.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "health_checks", indexes = {
    @Index(name = "idx_check_datetime", columnList = "check_datetime")
})
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id", nullable = false)
    private Long checkId;

    @Column(name = "check_datetime", nullable = false)
    private Instant checkDatetime;

    public HealthCheck() {
    }

    public HealthCheck(Instant checkDatetime) {
        this.checkDatetime = checkDatetime;
    }

    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }

    public Instant getCheckDatetime() {
        return checkDatetime;
    }

    public void setCheckDatetime(Instant checkDatetime) {
        this.checkDatetime = checkDatetime;
    }

    @Override
    public String toString() {
        return "HealthCheck{" +
                "checkId=" + checkId +
                ", checkDatetime=" + checkDatetime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthCheck that = (HealthCheck) o;
        return checkId != null && checkId.equals(that.checkId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}