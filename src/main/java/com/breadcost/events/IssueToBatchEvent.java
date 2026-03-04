package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * IssueToBatch event
 * Issues inventory to production batch
 * Produces FINANCIAL ledger entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueToBatchEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String batchId;
    private String itemId;
    private BigDecimal qty;
    private String uom;
    private String lotId;
    private String locationId;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String overrideReasonCode;
    private Boolean emergencyMode;
    private String approvedBy;
    private String approvalRef;
    private String exceptionId;

    @Override
    public String getEventType() {
        return "IssueToBatch";
    }
}
