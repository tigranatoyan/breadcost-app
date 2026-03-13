package com.breadcost.unit.projection;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import com.breadcost.projections.ProjectionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionEngineTest {

    private ProjectionEngine engine;
    private EventStore eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new EventStore(new com.fasterxml.jackson.databind.ObjectMapper());
        engine = new ProjectionEngine(eventStore);
        // Don't call initialize() to avoid @PostConstruct side effects; register manually
        eventStore.registerListener(engine);
    }

    @Test
    void initialWatermarks_zero() {
        assertEquals(0L, engine.getFinancialWatermark());
        assertEquals(0L, engine.getOperationalWatermark());
    }

    @Test
    void financialEvent_updatesFinancialWatermark() {
        eventStore.appendEvent(testEvent("ReceiveLot"), LedgerEntry.EntryClass.FINANCIAL);

        assertTrue(engine.getFinancialWatermark() > 0);
        assertEquals(0L, engine.getOperationalWatermark());
    }

    @Test
    void operationalEvent_updatesOperationalWatermark() {
        eventStore.appendEvent(testEvent("TransferInventory"), LedgerEntry.EntryClass.OPERATIONAL);

        assertEquals(0L, engine.getFinancialWatermark());
        assertTrue(engine.getOperationalWatermark() > 0);
    }

    @Test
    void hasProcessedFinancial_beforeEvent_false() {
        assertFalse(engine.hasProcessedFinancial(5000L));
    }

    @Test
    void hasProcessedFinancial_afterEvent_true() {
        Long seq = eventStore.appendEvent(testEvent("ReceiveLot"), LedgerEntry.EntryClass.FINANCIAL);

        assertTrue(engine.hasProcessedFinancial(seq));
    }

    @Test
    void multipleEvents_watermarkTracksMax() {
        Long seq1 = eventStore.appendEvent(testEvent("A"), LedgerEntry.EntryClass.FINANCIAL);
        Long seq2 = eventStore.appendEvent(testEvent("B"), LedgerEntry.EntryClass.FINANCIAL);

        assertEquals(seq2, engine.getFinancialWatermark());
        assertTrue(engine.hasProcessedFinancial(seq1));
        assertTrue(engine.hasProcessedFinancial(seq2));
    }

    private DomainEvent testEvent(String type) {
        return new DomainEvent() {
            public String getTenantId() { return "t1"; }
            public String getSiteId() { return "site-1"; }
            public Instant getOccurredAtUtc() { return Instant.now(); }
            public String getIdempotencyKey() { return "k-" + System.nanoTime(); }
            public String getEventType() { return type; }
        };
    }
}
