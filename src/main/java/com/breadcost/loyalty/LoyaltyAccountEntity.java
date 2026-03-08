package com.breadcost.loyalty;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Loyalty account per customer per tenant.
 * BC-E12: Loyalty module
 */
@Entity
@Table(
    name = "loyalty_accounts",
    uniqueConstraints = @UniqueConstraint(name = "uq_loyalty_tenant_customer",
        columnNames = {"tenantId", "customerId"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyAccountEntity {

    @Id
    private String accountId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    /** Total accumulated points (not yet redeemed) */
    @Builder.Default
    private long pointsBalance = 0L;

    /** Total lifetime points earned */
    @Builder.Default
    private long pointsEarned = 0L;

    /** Total lifetime points redeemed */
    @Builder.Default
    private long pointsRedeemed = 0L;

    /** Current tier name (denormalised for fast reads) */
    @Builder.Default
    private String tierName = "Bronze";

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
