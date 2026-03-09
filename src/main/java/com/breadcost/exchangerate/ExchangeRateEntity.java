package com.breadcost.exchangerate;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Exchange rate entity — BC-2201: Exchange rate API integration (FR-9.7)
 * Stores per-currency per-date rates. Source: MANUAL or API.
 */
@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = @UniqueConstraint(name = "uq_exchange_rate",
        columnNames = {"tenantId", "baseCurrency", "currencyCode", "rateDate"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateEntity {

    @Id
    private String rateId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String baseCurrency = "USD";

    @Column(nullable = false, length = 10)
    private String currencyCode;

    @Column(nullable = false)
    private double rate;

    @Column(nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String source = "MANUAL";

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
