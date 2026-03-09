package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * AI production suggestion — BC-1902 (FR-12.4)
 */
@Entity
@Table(name = "ai_production_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiProductionSuggestionEntity {

    @Id
    private String suggestionId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String productId;

    private String productName;

    private double suggestedQty;

    @Builder.Default
    private int suggestedBatches = 1;

    @Column(length = 500)
    private String reason;

    private LocalDate planDate;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
