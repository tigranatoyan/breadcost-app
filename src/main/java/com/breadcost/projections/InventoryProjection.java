package com.breadcost.projections;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.DomainEvent;
import com.breadcost.events.BackflushConsumptionEvent;
import com.breadcost.events.ReceiveLotEvent;
import com.breadcost.events.IssueToBatchEvent;
import com.breadcost.events.TransferInventoryEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

/**
 * In-memory projection for inventory valuation
 * Rebuilds from event store on startup
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryProjection {
    private final EventStore eventStore;
    private final Map<String, InventoryPosition> positions = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        eventStore.registerListener(this::handleEvent);
        rebuild();
    }

    /**
     * Rebuild projection from all events
     */
    public void rebuild() {
        log.info("Rebuilding inventory projection from event store");
        positions.clear();
        
        List<StoredEvent> events = eventStore.getAllEvents();
        for (StoredEvent storedEvent : events) {
            processEvent(storedEvent.getEvent());
        }
        
        log.info("Inventory projection rebuilt: {} positions", positions.size());
    }

    /**
     * Handle new event
     */
    private void handleEvent(StoredEvent storedEvent, LedgerEntry ledgerEntry) {
        processEvent(storedEvent.getEvent());
    }

    /**
     * Process single event
     */
    private void processEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "ReceiveLot":
                handleReceiveLot((ReceiveLotEvent) event);
                break;
            case "IssueToBatch":
                handleIssueToBatch((IssueToBatchEvent) event);
                break;
            case "TransferInventory":
                handleTransferInventory((TransferInventoryEvent) event);
                break;
            case "BackflushConsumption":
                handleBackflushConsumption((BackflushConsumptionEvent) event);
                break;
            default:
                // Ignore other event types for inventory projection
        }
    }

    private void handleReceiveLot(ReceiveLotEvent event) {
        String key = buildKey(event.getTenantId(), event.getSiteId(), 
                             event.getItemId(), event.getLotId(), "RECEIVING");
        
        InventoryPosition position = positions.computeIfAbsent(key, k -> 
            InventoryPosition.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(event.getTenantId())
                .siteId(event.getSiteId())
                .itemId(event.getItemId())
                .lotId(event.getLotId())
                .locationId("RECEIVING")
                .onHandQty(BigDecimal.ZERO)
                .valuationAmount(BigDecimal.ZERO)
                .avgUnitCost(BigDecimal.ZERO)
                .build()
        );

        // Update quantity and value using weighted average cost
        BigDecimal totalValue = position.valuationAmount.add(
            event.getUnitCostBase().multiply(event.getQty())
        );
        BigDecimal totalQty = position.onHandQty.add(event.getQty());
        
        position.onHandQty = totalQty;
        position.valuationAmount = totalValue;
        position.avgUnitCost = totalQty.compareTo(BigDecimal.ZERO) > 0 
            ? totalValue.divide(totalQty, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        position.uom = event.getUom();

        log.debug("Received lot: item={}, lot={}, qty={}, value={}", 
                 event.getItemId(), event.getLotId(), totalQty, totalValue);
    }

    private void handleIssueToBatch(IssueToBatchEvent event) {
        String key = buildKey(event.getTenantId(), event.getSiteId(), 
                             event.getItemId(), event.getLotId(), 
                             event.getLocationId() != null ? event.getLocationId() : "RECEIVING");
        
        InventoryPosition position = positions.get(key);
        if (position != null) {
            BigDecimal issueValue = position.avgUnitCost.multiply(event.getQty());
            position.onHandQty = position.onHandQty.subtract(event.getQty());
            position.valuationAmount = position.valuationAmount.subtract(issueValue);
            
            // Remove if quantity is zero
            if (position.onHandQty.compareTo(BigDecimal.ZERO) <= 0) {
                positions.remove(key);
            }
            
            log.debug("Issued to batch: item={}, qty={}, remaining={}", 
                     event.getItemId(), event.getQty(), position.onHandQty);
        }
    }

    private void handleTransferInventory(TransferInventoryEvent event) {
        String fromKey = buildKey(event.getTenantId(), event.getSiteId(), 
                                  event.getItemId(), event.getLotId(), event.getFromLocationId());
        String toKey = buildKey(event.getTenantId(), event.getSiteId(), 
                               event.getItemId(), event.getLotId(), event.getToLocationId());
        
        InventoryPosition fromPosition = positions.get(fromKey);
        if (fromPosition != null) {
            BigDecimal transferValue = fromPosition.avgUnitCost.multiply(event.getQty());
            BigDecimal avgCost = fromPosition.avgUnitCost;
            String uom = fromPosition.uom;
            
            // Remove from source
            fromPosition.onHandQty = fromPosition.onHandQty.subtract(event.getQty());
            fromPosition.valuationAmount = fromPosition.valuationAmount.subtract(transferValue);
            
            if (fromPosition.onHandQty.compareTo(BigDecimal.ZERO) <= 0) {
                positions.remove(fromKey);
            }
            
            // Add to destination
            InventoryPosition toPosition = positions.computeIfAbsent(toKey, k -> 
                InventoryPosition.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(event.getTenantId())
                    .siteId(event.getSiteId())
                    .itemId(event.getItemId())
                    .lotId(event.getLotId())
                    .locationId(event.getToLocationId())
                    .onHandQty(BigDecimal.ZERO)
                    .valuationAmount(BigDecimal.ZERO)
                    .avgUnitCost(avgCost)
                    .uom(uom)
                    .build()
            );
            
            toPosition.onHandQty = toPosition.onHandQty.add(event.getQty());
            toPosition.valuationAmount = toPosition.valuationAmount.add(transferValue);
        }
    }

    private void handleBackflushConsumption(BackflushConsumptionEvent event) {
        // Deduct from the first position that has stock for this item (FIFO across lots/locations)
        BigDecimal remaining = event.getQty();
        List<Map.Entry<String, InventoryPosition>> matching = positions.entrySet().stream()
                .filter(e -> event.getTenantId().equals(e.getValue().tenantId)
                        && event.getItemId().equals(e.getValue().itemId)
                        && e.getValue().onHandQty.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(e -> e.getValue().id)) // stable order
                .toList();

        for (Map.Entry<String, InventoryPosition> entry : matching) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            InventoryPosition pos = entry.getValue();
            BigDecimal deduct = remaining.min(pos.onHandQty);
            BigDecimal deductValue = pos.avgUnitCost.multiply(deduct);
            pos.onHandQty = pos.onHandQty.subtract(deduct);
            pos.valuationAmount = pos.valuationAmount.subtract(deductValue);
            remaining = remaining.subtract(deduct);
            if (pos.onHandQty.compareTo(BigDecimal.ZERO) <= 0) {
                positions.remove(entry.getKey());
            }
        }
        log.debug("Backflush consumption: item={}, qty={}, source={}, ref={}",
                event.getItemId(), event.getQty(), event.getSource(), event.getReferenceId());
    }

    /**
     * Apply a manual adjustment (positive = add, negative = reduce).
     * Used by /v1/inventory/adjust — FR-5.5
     */
    public void applyAdjustment(String tenantId, String siteId, String itemId,
                                 BigDecimal qty, String reasonCode) {
        // Find or create a synthetic position for adjustments
        String key = buildKey(tenantId, siteId != null ? siteId : "DEFAULT", itemId, "ADJ", "ADJUSTMENT");
        InventoryPosition position = positions.computeIfAbsent(key, k ->
                InventoryPosition.builder()
                        .id(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .siteId(siteId != null ? siteId : "DEFAULT")
                        .itemId(itemId)
                        .lotId("ADJ")
                        .locationId("ADJUSTMENT")
                        .onHandQty(BigDecimal.ZERO)
                        .valuationAmount(BigDecimal.ZERO)
                        .avgUnitCost(BigDecimal.ZERO)
                        .build()
        );
        position.onHandQty = position.onHandQty.add(qty);
        if (position.onHandQty.compareTo(BigDecimal.ZERO) <= 0) {
            positions.remove(key);
        }
        log.info("Inventory adjustment applied: item={}, qty={}, reason={}", itemId, qty, reasonCode);
    }

    /**
     * Get total on-hand quantity for an item across all lots/locations for a tenant.
     */
    public BigDecimal getTotalOnHand(String tenantId, String itemId) {
        return positions.values().stream()
                .filter(p -> tenantId.equals(p.tenantId) && itemId.equals(p.itemId))
                .map(p -> p.onHandQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get all inventory positions
     */
    public List<InventoryPosition> getAllPositions() {
        return new ArrayList<>(positions.values());
    }

    /**
     * Get positions for specific site
     */
    public List<InventoryPosition> getPositionsBySite(String siteId) {
        return positions.values().stream()
                .filter(p -> siteId == null || siteId.equals(p.siteId))
                .toList();
    }

    private String buildKey(String tenantId, String siteId, String itemId, String lotId, String locationId) {
        return String.format("%s:%s:%s:%s:%s", tenantId, siteId, itemId, 
                           lotId != null ? lotId : "NOLOT", locationId);
    }

    @Data
    @Builder
    public static class InventoryPosition {
        private String id;
        private String tenantId;
        private String siteId;
        private String itemId;
        private String lotId;
        private String locationId;
        private BigDecimal onHandQty;
        private String uom;
        private BigDecimal avgUnitCost;
        private BigDecimal valuationAmount;
    }
}
