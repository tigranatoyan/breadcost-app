package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_anomaly_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiAnomalyAlertEntity {

    @Id
    private String alertId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String alertType;

    @Builder.Default
    private String severity = "WARNING";

    private String metricName;

    @Column(precision = 18, scale = 4)
    private BigDecimal expectedValue;

    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Column(precision = 8, scale = 4)
    private BigDecimal deviationPct;

    @Column(length = 2000)
    private String explanation;

    @Column(length = 2000)
    private String suggestedAction;

    private String reportContext;

    @Builder.Default
    private String status = "ACTIVE";

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
