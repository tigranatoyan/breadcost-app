package com.breadcost.driver;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Packaging confirmation — BC-2102 (FR-8.7)
 */
@Entity
@Table(name = "packaging_confirmations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PackagingConfirmationEntity {

    @Id
    private String confirmationId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String runId;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    @Builder.Default
    private boolean allConfirmed = false;

    @Column(columnDefinition = "TEXT")
    private String discrepancies;

    private Instant confirmedAt;

    @PrePersist
    void onCreate() { confirmedAt = Instant.now(); }
}
