package com.breadcost.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Subscription tier definition. Seeded at startup for BASIC/STANDARD/ENTERPRISE.
 * BC-1701: Super-admin subscription tier assignment.
 */
@Entity
@Table(name = "subscription_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTierEntity {

    @Id
    private String tierId;

    /** BASIC, STANDARD, ENTERPRISE */
    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private TierLevel level;

    private String name;
    private String description;

    /**
     * Comma-delimited feature keys enabled for this tier.
     * e.g. "ORDERS,INVENTORY,REPORTS,DELIVERY,INVOICING,LOYALTY,AI_BOT"
     */
    @Column(length = 2000)
    private String enabledFeatures;

    /** Maximum number of users allowed (0 = unlimited). */
    @Builder.Default
    private int maxUsers = 0;

    /** Maximum number of tenants products (0 = unlimited). */
    @Builder.Default
    private int maxProducts = 0;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public List<String> featureList() {
        if (enabledFeatures == null || enabledFeatures.isBlank()) return List.of();
        return List.of(enabledFeatures.split(","));
    }

    public enum TierLevel {
        BASIC, STANDARD, ENTERPRISE
    }
}
