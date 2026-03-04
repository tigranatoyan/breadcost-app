package com.breadcost.purchaseorder;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Purchase order line — one ingredient per line.
 */
@Entity
@Table(name = "purchase_order_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderLineEntity {

    @Id
    private String lineId;

    @Column(nullable = false)
    private String poId;

    @Column(nullable = false)
    private String tenantId;

    private String ingredientId;
    private String ingredientName;

    @Builder.Default
    private double qty = 0;

    private String unit;

    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
