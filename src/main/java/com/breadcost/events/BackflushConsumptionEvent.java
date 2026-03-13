package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BackflushConsumption event
 * Automatically consumes ingredients per recipe during production or POS sale.
 * Each event represents one ingredient line being consumed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackflushConsumptionEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String batchId;
    private String recipeVersionId;
    /** The ingredient item being consumed */
    private String itemId;
    /** Quantity consumed (in purchasingUom) */
    private BigDecimal qty;
    /** Unit of measure */
    private String uom;
    /** Source: PRODUCTION or POS_SALE */
    private String source;
    /** Reference ID (workOrderId or saleId) */
    private String referenceId;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String lotSelectionRule;

    @Override
    public String getEventType() {
        return "BackflushConsumption";
    }
}
