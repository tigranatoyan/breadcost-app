package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * LedgerEntry entity
 * Immutable event-sourced ledger entry
 * Ordered by ledgerSeq for strict consistency
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    private String ledgerEntryId;
    private Long ledgerSeq;
    private String tenantId;
    private String siteId;
    private String periodId;
    private String policyVersionId;
    private EntryClass entryClass;
    private String eventType;
    private Instant occurredAtUtc;
    private Instant recordedAtUtc;
    private String idempotencyKey;
    
    // Financial posting fields
    private String accountDebitCode;
    private String accountCreditCode;
    
    // Inventory fields
    private String itemId;
    private String lotId;
    private String locationId;
    private String batchId;
    private BigDecimal qty;
    private String uom;
    private BigDecimal amountBase;
    
    // PPA fields
    private Boolean ppaFlag;
    private String ppaType;
    private String originalOccurredPeriodId;
    private Instant originalOccurredAtUtc;
    
    // Reversal tracking
    private String reversalOfId;

    public enum EntryClass {
        FINANCIAL,      // Affects financial watermark and period close
        OPERATIONAL     // Markers, transfers - does NOT delay period close
    }
}
