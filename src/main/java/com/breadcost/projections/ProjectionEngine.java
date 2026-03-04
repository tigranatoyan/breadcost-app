package com.breadcost.projections;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Projection engine
 * Consumes events from event store and updates read models
 * Tracks watermarks per READ_MODELS.yaml
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectionEngine implements EventStore.EventListener {
    private final EventStore eventStore;
    private final AtomicLong financialWatermark = new AtomicLong(0);
    private final AtomicLong operationalWatermark = new AtomicLong(0);

    @PostConstruct
    public void initialize() {
        log.info("Initializing projection engine");
        eventStore.registerListener(this);
    }

    @Override
    public void onEvent(StoredEvent event, LedgerEntry ledgerEntry) {
        log.debug("Processing event for projections: eventType={}, ledgerSeq={}, entryClass={}", 
                event.getEventType(), ledgerEntry.getLedgerSeq(), ledgerEntry.getEntryClass());

        try {
            // Update read models based on event
            updateProjections(event, ledgerEntry);

            // Update watermarks
            if (ledgerEntry.getEntryClass() == LedgerEntry.EntryClass.FINANCIAL) {
                financialWatermark.set(Math.max(financialWatermark.get(), ledgerEntry.getLedgerSeq()));
                log.debug("Financial watermark updated to: {}", financialWatermark.get());
            } else if (ledgerEntry.getEntryClass() == LedgerEntry.EntryClass.OPERATIONAL) {
                operationalWatermark.set(Math.max(operationalWatermark.get(), ledgerEntry.getLedgerSeq()));
                log.debug("Operational watermark updated to: {}", operationalWatermark.get());
            }
        } catch (Exception e) {
            log.error("Error processing event in projection: " + event.getEventType(), e);
        }
    }

    private void updateProjections(StoredEvent event, LedgerEntry ledgerEntry) {
        // This would update various read models:
        // - BatchCostView
        // - InventoryValuationView
        // - WIPView
        // - COGSBridgeView
        // - ExceptionQueueView
        // - ConfidenceView
        
        log.debug("Updating projections for event: {}", event.getEventType());
        // Implementation would dispatch to specific projection updaters
    }

    /**
     * Get current financial watermark
     * Used by ClosePeriod to ensure all financial entries processed
     */
    public Long getFinancialWatermark() {
        return financialWatermark.get();
    }

    /**
     * Get current operational watermark
     */
    public Long getOperationalWatermark() {
        return operationalWatermark.get();
    }

    /**
     * Check if projections have caught up to specific ledgerSeq
     */
    public boolean hasProcessedFinancial(Long ledgerSeq) {
        return financialWatermark.get() >= ledgerSeq;
    }
}
