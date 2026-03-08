package com.breadcost.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Single line item on an invoice.
 * BC-1501
 */
@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineEntity {

    @Id
    private String lineId;

    @Column(nullable = false)
    private String invoiceId;

    @Column(nullable = false)
    private String tenantId;

    private String productId;
    private String productName;

    @Builder.Default
    private BigDecimal qty = BigDecimal.ONE;

    private String unit;

    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Discount percentage applied (0-100). */
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
