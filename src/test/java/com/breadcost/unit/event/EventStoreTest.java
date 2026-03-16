package com.breadcost.unit.event;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventStoreTest {

    private EventStore eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new EventStore();
    }

    private DomainEvent testEvent(String tenantId, String type) {
        return new DomainEvent() {
            public String getTenantId() { return tenantId; }
            public String getSiteId() { return "site-1"; }
            public Instant getOccurredAtUtc() { return Instant.now(); }
            public String getIdempotencyKey() { return "key-" + System.nanoTime(); }
            public String getEventType() { return type; }
        };
    }

    @Test
    void appendEvent_returnsIncreasingLedgerSeq() {
        Long seq1 = eventStore.appendEvent(testEvent("t1", "TestEvent"), LedgerEntry.EntryClass.OPERATIONAL);
        Long seq2 = eventStore.appendEvent(testEvent("t1", "TestEvent"), LedgerEntry.EntryClass.OPERATIONAL);

        assertTrue(seq2 > seq1);
    }

    @Test
    void getAllEvents_returnsInOrder() {
        eventStore.appendEvent(testEvent("t1", "First"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "Second"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "Third"), LedgerEntry.EntryClass.OPERATIONAL);

        List<StoredEvent> all = eventStore.getAllEvents();

        assertEquals(3, all.size());
        assertEquals("First", all.get(0).getEventType());
        assertEquals("Second", all.get(1).getEventType());
        assertEquals("Third", all.get(2).getEventType());
    }

    @Test
    void getEventsAfter_filtersCorrectly() {
        Long seq1 = eventStore.appendEvent(testEvent("t1", "A"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "B"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "C"), LedgerEntry.EntryClass.OPERATIONAL);

        List<StoredEvent> after = eventStore.getEventsAfter(seq1);

        assertEquals(2, after.size());
        assertEquals("B", after.get(0).getEventType());
        assertEquals("C", after.get(1).getEventType());
    }

    @Test
    void appendEvent_notifiesListeners() {
        AtomicInteger callCount = new AtomicInteger(0);
        eventStore.registerListener((storedEvent, ledgerEntry) -> callCount.incrementAndGet());

        eventStore.appendEvent(testEvent("t1", "X"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "Y"), LedgerEntry.EntryClass.FINANCIAL);

        assertEquals(2, callCount.get());
    }

    @Test
    void appendEvent_listenerErrorDoesNotBlockStore() {
        eventStore.registerListener((storedEvent, ledgerEntry) -> {
            throw new RuntimeException("Listener failure");
        });

        Long seq = eventStore.appendEvent(testEvent("t1", "Safe"), LedgerEntry.EntryClass.OPERATIONAL);

        assertNotNull(seq);
        assertEquals(1, eventStore.getAllEvents().size());
    }

    @Test
    void getMaxLedgerSeq_returnsCorrectMax() {
        eventStore.appendEvent(testEvent("t1", "A"), LedgerEntry.EntryClass.OPERATIONAL);
        eventStore.appendEvent(testEvent("t1", "B"), LedgerEntry.EntryClass.FINANCIAL);
        Long last = eventStore.appendEvent(testEvent("t1", "C"), LedgerEntry.EntryClass.OPERATIONAL);

        Long max = eventStore.getMaxLedgerSeq(null);

        assertEquals(last, max);
    }

    @Test
    void emptyStore_getAllReturnsEmpty() {
        assertTrue(eventStore.getAllEvents().isEmpty());
        assertEquals(0L, eventStore.getMaxLedgerSeq(null));
    }
}
