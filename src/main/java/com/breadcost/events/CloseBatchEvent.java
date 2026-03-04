package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * CloseBatch event
 * Closes production batch and triggers RecognizeProduction
 * Does NOT produce ledger entry directly
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseBatchEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String batchId;
    private String closeMode;
    private Instant occurredAtUtc;
    private String idempotencyKey;

    @Override
    public String getEventType() {
        return "CloseBatch";
    }
}
