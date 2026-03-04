package com.breadcost.purchaseorder;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Purchase order — BC-1303, BC-1302, BC-1304, BC-1306
 *
 * Lifecycle: DRAFT → PENDING_APPROVAL → APPROVED → RECEIVED → CANCELLED
 */
@Entity
@Table(name = "purchase_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderEntity {

    @Id
    private String poId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String supplierId;

    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PoStatus status = PoStatus.DRAFT;

    @Column(length = 2000)
    private String notes;

    /** BC-1306: FX rate  */
    @Builder.Default
    private double fxRate = 1.0;

    @Builder.Default
    private String fxCurrencyCode = "USD";

    /** Total in base currency */
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String approvedBy;
    private Instant approvedAt;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }

    public enum PoStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, RECEIVED, CANCELLED
    }
}
