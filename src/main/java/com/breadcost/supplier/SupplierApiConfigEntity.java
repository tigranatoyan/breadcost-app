package com.breadcost.supplier;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Supplier API configuration — BC-2202: Supplier API integration (FR-6.4)
 * Stores per-supplier API connection settings for electronic PO submission.
 */
@Entity
@Table(
    name = "supplier_api_configs",
    uniqueConstraints = @UniqueConstraint(name = "uq_supplier_api_tenant",
        columnNames = {"tenantId", "supplierId"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierApiConfigEntity {

    @Id
    private String configId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String supplierId;

    @Column(length = 1000)
    private String apiUrl;

    /** Reference to externally stored API key (not the key itself). */
    private String apiKeyRef;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String format = "JSON";

    @Builder.Default
    private boolean enabled = false;

    private Instant lastSentAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
