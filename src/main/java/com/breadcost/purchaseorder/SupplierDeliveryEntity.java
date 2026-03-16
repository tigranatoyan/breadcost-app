package com.breadcost.purchaseorder;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Supplier delivery (goods received) — BC-1305: Delivery matching against PO
 */
@Entity
@Table(name = "supplier_deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierDeliveryEntity {

    @Id
    private String deliveryId;

    @Column(nullable = false)
    private String tenantId;

    /** The PO this delivery is matched against */
    private String poId;

    private String supplierId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.RECEIVED;

    /** True if any line has a discrepancy vs the PO */
    @Builder.Default
    private boolean hasDiscrepancy = false;

    @Column(length = 2000)
    private String notes;

    private Instant receivedAt;
    private Instant createdAt;

    @PrePersist void onCreate() {
        createdAt = Instant.now();
        if (receivedAt == null) receivedAt = createdAt;
    }

    public enum DeliveryStatus {
        RECEIVED, MATCHED, DISCREPANCY
    }
}
