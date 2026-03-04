package com.breadcost.loyalty;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Single loyalty point transaction (earn or redeem event).
 * BC-1201, BC-1204
 */
@Entity
@Table(name = "loyalty_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyTransactionEntity {

    @Id
    private String txId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    /** EARN or REDEEM */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private long points;

    /** Associated order, if any */
    private String orderId;

    private String description;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
