package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * AI demand forecast — BC-1903 (FR-12.7)
 */
@Entity
@Table(name = "ai_demand_forecasts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiDemandForecastEntity {

    @Id
    private String forecastId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String productId;

    private String productName;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    private double forecastQty;

    private Double confidence;

    @Builder.Default
    private int basedOnDays = 30;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
