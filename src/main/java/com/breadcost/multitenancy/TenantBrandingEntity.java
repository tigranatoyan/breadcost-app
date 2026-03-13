package com.breadcost.multitenancy;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * D4.3 — Tenant branding configuration (logo, colors, receipt template).
 */
@Entity
@Table(name = "tenant_branding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantBrandingEntity {

    @Id
    private String tenantId;

    private String logoUrl;

    @Builder.Default
    private String primaryColor = "#2563eb";

    @Builder.Default
    private String secondaryColor = "#1e40af";

    @Builder.Default
    private String accentColor = "#f59e0b";

    /** Custom business name shown on receipts */
    private String receiptBusinessName;

    /** Receipt footer text (address, phone, etc.) */
    @Column(columnDefinition = "TEXT")
    private String receiptFooter;

    /** Receipt header text or tagline */
    private String receiptHeader;

    /** Locale for the tenant UI (e.g., en, hy, uz) */
    @Builder.Default
    private String locale = "en";

    /** Timezone identifier (e.g., Asia/Tashkent) */
    @Builder.Default
    private String timezone = "UTC";

    private Instant updatedAt;

    @PrePersist @PreUpdate
    void prePersist() { updatedAt = Instant.now(); }
}
