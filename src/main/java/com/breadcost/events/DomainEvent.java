package com.breadcost.events;

import java.time.Instant;

/**
 * Base interface for all domain events
 * All events must include idempotencyKey for ledger entries
 */
public interface DomainEvent {
    String getTenantId();
    String getSiteId();
    Instant getOccurredAtUtc();
    String getIdempotencyKey();
    String getEventType();
}
