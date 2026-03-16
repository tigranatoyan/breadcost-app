package com.breadcost.unit.projection;

import com.breadcost.domain.LedgerEntry;
import com.breadcost.events.BackflushConsumptionEvent;
import com.breadcost.events.IssueToBatchEvent;
import com.breadcost.events.ReceiveLotEvent;
import com.breadcost.events.TransferInventoryEvent;
import com.breadcost.eventstore.EventStore;
import com.breadcost.projections.InventoryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InventoryProjectionTest {

    private EventStore eventStore;
    private InventoryProjection projection;

    @BeforeEach
    void setUp() {
        eventStore = new EventStore();
        projection = new InventoryProjection(eventStore);
        // Manually register listener + rebuild (mirrors @PostConstruct)
        eventStore.registerListener((storedEvent, ledgerEntry) ->
                // Use reflection-free approach: just track via rebuild after each event
                {});
        // We'll call rebuild() explicitly after appending events
    }

    private void receiveLot(String tenantId, String siteId, String itemId, String lotId,
                            BigDecimal qty, BigDecimal unitCost, String uom) {
        eventStore.appendEvent(ReceiveLotEvent.builder()
                .tenantId(tenantId).siteId(siteId).itemId(itemId).lotId(lotId)
                .qty(qty).unitCostBase(unitCost).uom(uom)
                .occurredAtUtc(Instant.now())
                .idempotencyKey("k-" + System.nanoTime())
                .build(), LedgerEntry.EntryClass.FINANCIAL);
        projection.rebuild();
    }

    private void issueToBatch(String tenantId, String siteId, String itemId, String lotId,
                              BigDecimal qty, String locationId) {
        eventStore.appendEvent(IssueToBatchEvent.builder()
                .tenantId(tenantId).siteId(siteId).itemId(itemId).lotId(lotId)
                .qty(qty).batchId("batch-1").locationId(locationId)
                .occurredAtUtc(Instant.now())
                .idempotencyKey("k-" + System.nanoTime())
                .build(), LedgerEntry.EntryClass.FINANCIAL);
        projection.rebuild();
    }

    private void transferInventory(String tenantId, String siteId, String itemId, String lotId,
                                   BigDecimal qty, String from, String to) {
        eventStore.appendEvent(TransferInventoryEvent.builder()
                .tenantId(tenantId).siteId(siteId).itemId(itemId).lotId(lotId)
                .qty(qty).fromLocationId(from).toLocationId(to)
                .occurredAtUtc(Instant.now())
                .idempotencyKey("k-" + System.nanoTime())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);
        projection.rebuild();
    }

    private void backflush(String tenantId, String siteId, String itemId, BigDecimal qty) {
        eventStore.appendEvent(BackflushConsumptionEvent.builder()
                .tenantId(tenantId).siteId(siteId).itemId(itemId)
                .qty(qty).source("PRODUCTION").referenceId("wo-1")
                .occurredAtUtc(Instant.now())
                .idempotencyKey("k-" + System.nanoTime())
                .build(), LedgerEntry.EntryClass.OPERATIONAL);
        projection.rebuild();
    }

    // ── ReceiveLot ───────────────────────────────────────────────────────────

    @Test
    void receiveLot_addsPosition() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.50"), "KG");

        assertEquals(bd("100"), projection.getTotalOnHand("t1", "flour"));
    }

    @Test
    void receiveLot_multipleLotsAccumulate() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        receiveLot("t1", "s1", "flour", "L2", bd("50"), bd("3.00"), "KG");

        // Total across lots
        assertEquals(bd("150"), projection.getTotalOnHand("t1", "flour"));
    }

    @Test
    void receiveLot_weightedAverageCost() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");

        var positions = projection.getAllPositions();
        var pos = positions.stream()
                .filter(p -> "flour".equals(p.getItemId()) && "L1".equals(p.getLotId()))
                .findFirst().orElseThrow();
        assertEquals(bd("2.0000"), pos.getAvgUnitCost());
        assertEquals(bd("200.00"), pos.getValuationAmount().setScale(2));
    }

    // ── IssueToBatch ─────────────────────────────────────────────────────────

    @Test
    void issueToBatch_reducesStock() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        issueToBatch("t1", "s1", "flour", "L1", bd("30"), "RECEIVING");

        assertEquals(bd("70"), projection.getTotalOnHand("t1", "flour"));
    }

    @Test
    void issueToBatch_fullDepletion_removesPosition() {
        receiveLot("t1", "s1", "sugar", "L1", bd("50"), bd("1.00"), "KG");
        issueToBatch("t1", "s1", "sugar", "L1", bd("50"), "RECEIVING");

        assertEquals(bd("0"), projection.getTotalOnHand("t1", "sugar"));
        assertTrue(projection.getAllPositions().stream()
                .noneMatch(p -> "sugar".equals(p.getItemId()) && "L1".equals(p.getLotId())));
    }

    // ── TransferInventory ────────────────────────────────────────────────────

    @Test
    void transfer_movesStock() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        transferInventory("t1", "s1", "flour", "L1", bd("40"), "RECEIVING", "PRODUCTION");

        // Total on hand unchanged
        assertEquals(bd("100"), projection.getTotalOnHand("t1", "flour"));
        // Positions split between locations
        assertEquals(2, projection.getAllPositions().stream()
                .filter(p -> "flour".equals(p.getItemId())).count());
    }

    @Test
    void transfer_fullAmount_removesSource() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        transferInventory("t1", "s1", "flour", "L1", bd("100"), "RECEIVING", "PRODUCTION");

        assertEquals(bd("100"), projection.getTotalOnHand("t1", "flour"));
        // Source position should be removed, only destination remains
        var positions = projection.getAllPositions().stream()
                .filter(p -> "flour".equals(p.getItemId())).toList();
        assertEquals(1, positions.size());
        assertEquals("PRODUCTION", positions.get(0).getLocationId());
    }

    // ── BackflushConsumption (FIFO) ──────────────────────────────────────────

    @Test
    void backflush_reducesStock() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        backflush("t1", "s1", "flour", bd("25"));

        assertEquals(bd("75"), projection.getTotalOnHand("t1", "flour"));
    }

    @Test
    void backflush_crossLotFifo() {
        receiveLot("t1", "s1", "flour", "L1", bd("30"), bd("2.00"), "KG");
        receiveLot("t1", "s1", "flour", "L2", bd("50"), bd("3.00"), "KG");

        // Consume 40: should take 30 from L1 + 10 from L2
        backflush("t1", "s1", "flour", bd("40"));

        assertEquals(bd("40"), projection.getTotalOnHand("t1", "flour"));
    }

    // ── applyAdjustment ──────────────────────────────────────────────────────

    @Test
    void adjustment_positive_addsStock() {
        projection.applyAdjustment("t1", "s1", "flour", bd("50"), "COUNT");

        assertEquals(bd("50"), projection.getTotalOnHand("t1", "flour"));
    }

    @Test
    void adjustment_negative_removesPosition() {
        projection.applyAdjustment("t1", "s1", "flour", bd("50"), "COUNT");
        projection.applyAdjustment("t1", "s1", "flour", bd("-50"), "CORRECTION");

        assertEquals(bd("0"), projection.getTotalOnHand("t1", "flour"));
    }

    // ── getTotalOnHand ───────────────────────────────────────────────────────

    @Test
    void onHand_differentTenants_isolated() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        receiveLot("t2", "s1", "flour", "L1", bd("50"), bd("2.00"), "KG");

        assertEquals(bd("100"), projection.getTotalOnHand("t1", "flour"));
        assertEquals(bd("50"), projection.getTotalOnHand("t2", "flour"));
    }

    @Test
    void onHand_unknownItem_zero() {
        assertEquals(bd("0"), projection.getTotalOnHand("t1", "nonexistent"));
    }

    // ── rebuild ──────────────────────────────────────────────────────────────

    @Test
    void rebuild_clearsThenReprocesses() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        int sizeBefore = projection.getAllPositions().size();

        projection.rebuild();

        assertEquals(sizeBefore, projection.getAllPositions().size());
        assertEquals(bd("100"), projection.getTotalOnHand("t1", "flour"));
    }

    // ── getPositionsBySite ───────────────────────────────────────────────────

    @Test
    void positionsBySite_filters() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        receiveLot("t1", "s2", "sugar", "L1", bd("50"), bd("1.00"), "KG");

        assertEquals(1, projection.getPositionsBySite("s1").size());
        assertEquals(1, projection.getPositionsBySite("s2").size());
    }

    @Test
    void positionsBySite_nullReturnsAll() {
        receiveLot("t1", "s1", "flour", "L1", bd("100"), bd("2.00"), "KG");
        receiveLot("t1", "s2", "sugar", "L1", bd("50"), bd("1.00"), "KG");

        assertEquals(2, projection.getPositionsBySite(null).size());
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
