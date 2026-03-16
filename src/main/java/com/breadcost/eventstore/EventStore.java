package com.breadcost.eventstore;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory event store implementation
 * Provides strict ordering via ledgerSeq
 * Stores events and generates ledger entries
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventStore implements EventPublisher {
    private final Map<Long, StoredEvent> eventsByLedgerSeq = new ConcurrentHashMap<>();
    private final Map<String, LedgerEntry> ledgerEntriesById = new ConcurrentHashMap<>();
    private final AtomicLong ledgerSeqGenerator = new AtomicLong(1000);
    private final List<EventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Append event to store and generate ledger entry
     * Returns ledgerSeq for the new entry
     */
    public synchronized Long appendEvent(DomainEvent event, LedgerEntry.EntryClass entryClass) {
        Long ledgerSeq = ledgerSeqGenerator.incrementAndGet();
        String ledgerEntryId = UUID.randomUUID().toString();
        
        StoredEvent storedEvent = StoredEvent.builder()
                .ledgerSeq(ledgerSeq)
                .event(event)
                .eventType(event.getEventType())
                .recordedAtUtc(Instant.now())
                .build();
        
        eventsByLedgerSeq.put(ledgerSeq, storedEvent);
        
        // Generate ledger entry
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .ledgerEntryId(ledgerEntryId)
                .ledgerSeq(ledgerSeq)
                .tenantId(event.getTenantId())
                .siteId(event.getSiteId())
                .entryClass(entryClass)
                .eventType(event.getEventType())
                .occurredAtUtc(event.getOccurredAtUtc())
                .recordedAtUtc(storedEvent.getRecordedAtUtc())
                .idempotencyKey(event.getIdempotencyKey())
                .build();
        
        ledgerEntriesById.put(ledgerEntryId, ledgerEntry);
        
        log.info("Event appended: eventType={}, ledgerSeq={}, entryClass={}", 
                event.getEventType(), ledgerSeq, entryClass);
        
        // Notify listeners asynchronously
        notifyListeners(storedEvent, ledgerEntry);
        
        return ledgerSeq;
    }

    @Override
    public Long publish(DomainEvent event, LedgerEntry.EntryClass entryClass) {
        return appendEvent(event, entryClass);
    }

    /**
     * Get all events in ledgerSeq order
     */
    public List<StoredEvent> getAllEvents() {
        return eventsByLedgerSeq.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Get events after specific ledgerSeq
     */
    public List<StoredEvent> getEventsAfter(Long afterLedgerSeq) {
        return eventsByLedgerSeq.entrySet().stream()
                .filter(e -> e.getKey() > afterLedgerSeq)
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Get ledger entries for specific period and entry class
     */
    public List<LedgerEntry> getLedgerEntriesForPeriod(String periodId, LedgerEntry.EntryClass entryClass) {
        return ledgerEntriesById.values().stream()
                .filter(e -> periodId.equals(e.getPeriodId()))
                .filter(e -> entryClass == null || entryClass == e.getEntryClass())
                .sorted(Comparator.comparing(LedgerEntry::getLedgerSeq))
                .toList();
    }

    /**
     * Get maximum ledgerSeq for entry class
     */
    public Long getMaxLedgerSeq(LedgerEntry.EntryClass entryClass) {
        return ledgerEntriesById.values().stream()
                .filter(e -> entryClass == null || entryClass == e.getEntryClass())
                .map(LedgerEntry::getLedgerSeq)
                .max(Long::compareTo)
                .orElse(0L);
    }

    /**
     * Register event listener
     */
    public void registerListener(EventListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(StoredEvent storedEvent, LedgerEntry ledgerEntry) {
        listeners.forEach(listener -> {
            try {
                listener.onEvent(storedEvent, ledgerEntry);
            } catch (Exception e) {
                log.error("Error notifying listener: " + listener.getClass().getSimpleName(), e);
            }
        });
    }

    /**
     * Listener interface for event notifications
     */
    public interface EventListener {
        void onEvent(StoredEvent event, LedgerEntry ledgerEntry);
    }
}
