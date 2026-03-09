package com.breadcost.ai;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_pricing_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiPricingSuggestionEntity {

    @Id
    private String suggestionId;

    @Column(nullable = false)
    private String tenantId;

    private String productId;
    private String productName;
    private String customerId;
    private String customerName;

    @Column(precision = 18, scale = 4)
    private BigDecimal currentPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal suggestedPrice;

    @Column(precision = 8, scale = 4)
    private BigDecimal changePct;

    @Column(length = 2000)
    private String reason;

    @Builder.Default
    private String status = "PENDING";

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
