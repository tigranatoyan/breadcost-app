package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ReceiveLot event
 * Records receipt of inventory with initial cost
 * Produces FINANCIAL ledger entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveLotEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String receiptId;
    private String itemId;
    private String lotId;
    private BigDecimal qty;
    private String uom;
    private BigDecimal unitCostBase;
    private Instant occurredAtUtc;
    private String idempotencyKey;

    @Override
    public String getEventType() {
        return "ReceiveLot";
    }
}
