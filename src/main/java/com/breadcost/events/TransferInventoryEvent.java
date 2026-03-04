package com.breadcost.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TransferInventory event
 * Moves inventory between locations
 * Produces OPERATIONAL ledger entry (amountBase=0)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferInventoryEvent implements DomainEvent {
    private String tenantId;
    private String siteId;
    private String itemId;
    private BigDecimal qty;
    private String fromLocationId;
    private String toLocationId;
    private Instant occurredAtUtc;
    private String idempotencyKey;
    private String lotId;

    @Override
    public String getEventType() {
        return "TransferInventory";
    }
}
