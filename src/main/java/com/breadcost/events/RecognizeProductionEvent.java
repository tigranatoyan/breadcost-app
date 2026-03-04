package com.breadcost.events;

import com.breadcost.domain.RecognitionOutputSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * RecognizeProduction event
 * Recognizes finished goods from batch and relieves WIP
 * Produces FINANCIAL ledger entries
 * Internal job triggered by CloseBatch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognizeProductionEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String batchId;
    private BigDecimal wipBalanceAmountBase;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private List<RecognitionOutputSet.Output> outputs;
    private String recognitionOutputSetId;

    @Override
    public String getEventType() {
        return "RecognizeProduction";
    }
}
