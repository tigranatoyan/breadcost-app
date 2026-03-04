package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * LateEntryNotEligibleForFGAdj marker event
 * OPERATIONAL marker (amountBase=0, qty=null)
 * Indicates late entry that doesn't trigger FG adjustment
 * Markers excluded from financial close cutoff per CLOSE_BLOCKING_LAW
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateEntryNotEligibleForFGAdjEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private Long sourceLedgerSeq;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String reason;

    @Override
    public String getEventType() {
        return "LateEntryNotEligibleForFGAdj";
    }
}
