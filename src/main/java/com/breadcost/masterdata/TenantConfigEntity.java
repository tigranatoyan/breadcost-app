package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tenant_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfigEntity {

    @Id
    private String tenantId;

    private String displayName;

    /** 24h format HH:mm — default "22:00" */
    @Builder.Default
    private String orderCutoffTime = "22:00";

    /** Rush order premium percent — default 15 */
    @Builder.Default
    private double rushOrderPremiumPct = 15.0;

    /** ISO 4217 currency code */
    @Builder.Default
    private String mainCurrency = "UZS";

    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
