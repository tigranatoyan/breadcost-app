package com.breadcost.driver;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Driver on-spot payment — BC-2103 (FR-8.8)
 */
@Entity
@Table(name = "driver_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverPaymentEntity {

    @Id
    private String paymentId;

    @Column(nullable = false)
    private String tenantId;

    private String sessionId;

    @Column(nullable = false)
    private String orderId;

    private String invoiceId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String paymentMethod = "CASH";

    private String reference;

    private Instant collectedAt;

    @PrePersist
    void onCreate() { collectedAt = Instant.now(); }
}
