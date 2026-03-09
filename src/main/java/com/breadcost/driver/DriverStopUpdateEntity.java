package com.breadcost.driver;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Driver stop update — BC-2101 (FR-7.7)
 */
@Entity
@Table(name = "driver_stop_updates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverStopUpdateEntity {

    @Id
    private String updateId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String runOrderId;

    /** ARRIVED, DELIVERED, FAILED */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String notes;

    private Double lat;
    private Double lng;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
