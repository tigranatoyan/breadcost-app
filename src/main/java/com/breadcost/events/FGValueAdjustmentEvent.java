package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * FGValueAdjustment event
 * Adjusts FG value based on late entries after recognition
 * Produces FINANCIAL ledger entry
 * Internal job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FGValueAdjustmentEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String batchId;
    private String recognitionOutputSetId;
    private BigDecimal amountBase;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String allocationBasis;
    private Long sourceLedgerSeq;

    @Override
    public String getEventType() {
        return "FG_VALUE_ADJUSTMENT";
    }
}
