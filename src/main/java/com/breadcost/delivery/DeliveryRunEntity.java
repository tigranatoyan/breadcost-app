package com.breadcost.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Delivery run — BC-1401/1402/1403/1405/1406
 *
 * A delivery run groups orders assigned to a driver for a given date.
 */
@Entity
@Table(name = "delivery_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryRunEntity {

    @Id
    private String runId;

    @Column(nullable = false)
    private String tenantId;

    private String driverId;
    private String driverName;

    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RunStatus status = RunStatus.PENDING;

    /** Total courier charge for the run */
    @Builder.Default
    private BigDecimal courierCharge = BigDecimal.ZERO;

    @Column(length = 2000)
    private String notes;

    private Integer runNumber;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate()  { updatedAt = Instant.now(); }

    public enum RunStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
