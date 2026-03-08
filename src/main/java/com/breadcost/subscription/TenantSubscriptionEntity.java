package com.breadcost.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A tenant's current subscription tier assignment.
 * BC-1701: Super-admin subscription tier assignment.
 */
@Entity
@Table(name = "tenant_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSubscriptionEntity {

    @Id
    private String subscriptionId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionTierEntity.TierLevel tierLevel;

    private LocalDate startDate;
    private LocalDate expiryDate;

    @Builder.Default
    private boolean active = true;

    private String assignedBy;
    private Instant assignedAt;

    @PrePersist
    void prePersist() {
        this.assignedAt = Instant.now();
    }
}
