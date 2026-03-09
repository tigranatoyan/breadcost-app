package com.breadcost.driver;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Driver session — BC-2101 (FR-7.7)
 */
@Entity
@Table(name = "driver_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverSessionEntity {

    @Id
    private String sessionId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String driverId;

    private String driverName;

    private String runId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    private Instant startedAt;
    private Instant endedAt;

    private Double lat;
    private Double lng;

    private Instant updatedAt;

    @PrePersist
    void onCreate() { startedAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
