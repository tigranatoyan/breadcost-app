package com.breadcost.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One line in an Order — a specific product with quantity.
 */
@Getter
@Builder
public class OrderLine {

    private final String orderLineId;

    private final String productId;
    private final String productName;
    private final String departmentId;
    private final String departmentName;

    private final double qty;
    private final String uom;

    /** Price per unit BEFORE rush premium */
    private final BigDecimal unitPrice;

    /**
     * True when the product's department lead time means it cannot be ready
     * by requestedDeliveryTime — informational flag; does not block the order.
     */
    private final boolean leadTimeConflict;

    /** Earliest time this line's department can fulfil given its lead time (nullable if no conflict) */
    private final Instant earliestReadyAt;

    /** Notes specific to this line (e.g. special instructions, alternative packaging) */
    private final String notes;
}
