package com.breadcost.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Order aggregate — represents a B2B customer order placed by an operator.
 * Statuses: DRAFT → CONFIRMED → IN_PRODUCTION → READY → OUT_FOR_DELIVERY → DELIVERED | CANCELLED
 */
@Getter
@Builder
@With
public class Order {

    public enum Status {
        DRAFT,
        CONFIRMED,
        IN_PRODUCTION,
        READY,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }

    private final String orderId;
    private final String tenantId;
    private final String siteId;

    /** The customer this order is for */
    private final String customerId;
    private final String customerName;

    /** Operator who created / manages the order */
    private final String createdByUserId;

    private final Status status;

    private final Instant requestedDeliveryTime;
    private final Instant orderPlacedAt;
    private final Instant confirmedAt;

    /** True if placed after the daily cutoff — a premium may apply */
    private final boolean rushOrder;

    /** Rush premium percentage applied to all lines (0 if not rush) */
    private final BigDecimal rushPremiumPct;

    /** Free-text notes from the operator or customer */
    private final String notes;

    private final List<OrderLine> lines;

    /** Computed: sum of (line unit price * qty * (1 + rushPremiumPct)) */
    public BigDecimal totalAmount() {
        if (lines == null || lines.isEmpty()) return BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE.add(
                rushPremiumPct != null ? rushPremiumPct.divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO
        );
        return lines.stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQty())).multiply(multiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isCancellable() {
        return status == Status.DRAFT || status == Status.CONFIRMED;
    }

    public boolean isModifiable() {
        return status == Status.DRAFT;
    }
}
