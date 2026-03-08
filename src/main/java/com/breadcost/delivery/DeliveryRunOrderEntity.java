package com.breadcost.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order within a delivery run — BC-1401/1403/1404/1405/1406
 */
@Entity
@Table(name = "delivery_run_orders",
       uniqueConstraints = @UniqueConstraint(name = "uq_delivery_run_order",
           columnNames = {"runId", "orderId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryRunOrderEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String runId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderDeliveryStatus status = OrderDeliveryStatus.PENDING;

    /** Reason if delivery failed */
    private String failureReason;

    /** Re-delivery run id (if failed and reassigned) */
    private String reDeliveryRunId;

    /** Courier charge share for this order (BC-1405) */
    @Builder.Default
    private BigDecimal courierCharge = BigDecimal.ZERO;

    /** BC-1406: waived flag */
    @Builder.Default
    private boolean courierChargeWaived = false;

    /** BC-1406: who authorised the waiver */
    private String waivedBy;

    private Instant completedAt;
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public enum OrderDeliveryStatus {
        PENDING, COMPLETED, FAILED
    }
}
