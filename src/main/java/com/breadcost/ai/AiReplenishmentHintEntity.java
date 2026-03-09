package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AI replenishment hint — BC-1901 (FR-12.3)
 */
@Entity
@Table(name = "ai_replenishment_hints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiReplenishmentHintEntity {

    @Id
    private String hintId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String itemId;

    private String itemName;

    @Builder.Default
    private double currentQty = 0;

    @Builder.Default
    private double avgDailyUse = 0;

    private Double daysLeft;

    @Builder.Default
    private double suggestedQty = 0;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String period = "WEEKLY";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
