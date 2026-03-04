package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * BackflushConsumption event
 * Automatically consumes ingredients per recipe during production
 * Produces FINANCIAL ledger entries
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
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String lotSelectionRule;

    @Override
    public String getEventType() {
        return "BackflushConsumption";
    }
}
