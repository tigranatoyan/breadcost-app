package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * D3.4 — Quality prediction entity storing ML-like quality risk assessments.
 */
@Entity
@Table(name = "ai_quality_predictions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiQualityPredictionEntity {

    @Id
    private String predictionId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String productId;

    private String productName;
    private String recipeId;

    /** Predicted quality risk: LOW, MEDIUM, HIGH */
    @Column(nullable = false)
    private String riskLevel;

    /** Predicted yield percentage (0–100) */
    private double predictedYieldPct;

    /** Historical average yield for this product */
    private double historicalAvgYieldPct;

    /** Primary risk factor explanation */
    @Column(columnDefinition = "TEXT")
    private String riskFactors;

    /** Recommended action */
    @Column(columnDefinition = "TEXT")
    private String recommendation;

    /** Confidence score 0–1 */
    private double confidence;

    @Builder.Default
    private String status = "ACTIVE";

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
