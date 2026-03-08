package com.breadcost.purchaseorder;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One delivery line — BC-1305
 * Records actual quantity received vs ordered, flags discrepancy.
 */
@Entity
@Table(name = "supplier_delivery_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierDeliveryLineEntity {

    @Id
    private String lineId;

    @Column(nullable = false)
    private String deliveryId;

    @Column(nullable = false)
    private String tenantId;

    private String ingredientId;
    private String ingredientName;

    @Builder.Default
    private double qtyOrdered = 0;

    @Builder.Default
    private double qtyReceived = 0;

    private String unit;

    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** True if qtyReceived != qtyOrdered */
    @Builder.Default
    private boolean discrepancy = false;

    private String discrepancyNote;
}
