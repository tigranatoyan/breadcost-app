package com.breadcost.loyalty;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Loyalty tier configuration — BC-1202, BC-1203, BC-1206
 *
 * Example: Bronze (0+), Silver (1000+), Gold (5000+)
 * Benefits: discountPct applied at checkout when redeeming (BC-1204).
 */
@Entity
@Table(
    name = "loyalty_tiers",
    uniqueConstraints = @UniqueConstraint(name = "uq_loyalty_tier_tenant_name",
        columnNames = {"tenantId", "name"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyTierEntity {

    @Id
    private String tierId;

    @Column(nullable = false)
    private String tenantId;

    /** Display name: Bronze, Silver, Gold, Platinum… */
    @Column(nullable = false)
    private String name;

    /** Minimum lifetime points to qualify for this tier */
    @Builder.Default
    private long minPoints = 0L;

    /** Extra discount percentage benefit for this tier (e.g., 5.0 = 5% off) */
    @Builder.Default
    private double discountPct = 0.0;

    /** Points earned per $1 spent (multiplier for this tier) */
    @Builder.Default
    private double pointsPerDollar = 1.0;

    /** Additional description of tier benefits */
    private String benefitsDescription;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
