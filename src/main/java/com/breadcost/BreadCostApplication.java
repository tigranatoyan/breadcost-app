package com.breadcost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BreadCost Manufacturing Cost Accounting System
 * 
 * Event-sourced CQRS application for manufacturing cost accounting.
 * Implements event sourcing with command handlers, event store, and projections.
 * 
 * Architecture:
 * - Commands flow through REST API to CommandHandlers
 * - CommandHandlers validate, check idempotency, and emit events to EventStore
 * - Events are persisted with ledgerSeq ordering
 * - Projections consume events to build read models
 * - Queries are served from read model projections
 */
@SpringBootApplication
public class BreadCostApplication {

    public static void main(String[] args) {
        SpringApplication.run(BreadCostApplication.class, args);
    }
}
