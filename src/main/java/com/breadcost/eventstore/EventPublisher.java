package com.breadcost.eventstore;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;

/**
 * Abstraction for publishing domain events.
 * Implementations: in-memory EventStore (default), RabbitMQ (rabbit profile).
 */
public interface EventPublisher {

    /**
     * Publish a domain event. May be routed to in-memory listeners or to a message broker.
     *
     * @param event      the domain event
     * @param entryClass FINANCIAL or OPERATIONAL
     * @return the assigned ledger sequence number
     */
    Long publish(DomainEvent event, LedgerEntry.EntryClass entryClass);
}
