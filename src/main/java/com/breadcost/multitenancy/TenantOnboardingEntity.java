package com.breadcost.multitenancy;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * D4.1 — Tenant onboarding request tracking.
 */
@Entity
@Table(name = "tenant_onboarding_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantOnboardingEntity {

    @Id
    private String requestId;

    /** Desired tenant identifier (slug) */
    @Column(nullable = false, unique = true)
    private String tenantSlug;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerEmail;

    private String ownerName;
    private String ownerPhone;
    private String country;
    private String currency;

    /** Subscription tier requested: BASIC, STANDARD, ENTERPRISE */
    @Builder.Default
    private String requestedTier = "BASIC";

    /** Status: PENDING, APPROVED, PROVISIONED, REJECTED */
    @Builder.Default
    private String status = "PENDING";

    private String rejectionReason;

    /** The actual tenantId assigned after provisioning */
    private String provisionedTenantId;

    private Instant createdAt;
    private Instant approvedAt;
    private Instant provisionedAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
