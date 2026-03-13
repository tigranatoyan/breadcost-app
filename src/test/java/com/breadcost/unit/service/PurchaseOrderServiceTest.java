package com.breadcost.unit.service;

import com.breadcost.masterdata.ProductionPlanService;
import com.breadcost.purchaseorder.*;
import com.breadcost.supplier.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository poRepo;
    @Mock private PurchaseOrderLineRepository lineRepo;
    @Mock private SupplierDeliveryRepository deliveryRepo;
    @Mock private SupplierDeliveryLineRepository deliveryLineRepo;
    @Mock private SupplierRepository supplierRepo;
    @Mock private SupplierCatalogItemRepository catalogItemRepo;
    @Mock private ProductionPlanService planService;
    @InjectMocks private PurchaseOrderService svc;

    // ── createPO ─────────────────────────────────────────────────────────────

    @Test
    void createPO_calculatesTotalAndSetsStatus() {
        when(supplierRepo.findById("s1")).thenReturn(Optional.of(
                SupplierEntity.builder().supplierId("s1").name("Baker Supply").build()));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var lines = List.of(
                new PurchaseOrderService.LineInput("flour", "Flour", 10, "KG", new BigDecimal("2.50"), "USD"),
                new PurchaseOrderService.LineInput("sugar", "Sugar", 5, "KG", new BigDecimal("3.00"), "USD"));

        var po = svc.createPO("t1", "s1", lines, "test", 1.0, "USD");

        assertEquals(PurchaseOrderEntity.PoStatus.PENDING_APPROVAL, po.getStatus());
        // 10*2.50 + 5*3.00 = 25+15 = 40
        assertEquals(0, new BigDecimal("40.00").compareTo(po.getTotalAmount()));
        assertEquals("Baker Supply", po.getSupplierName());
    }

    @Test
    void createPO_nullLines_zeroTotal() {
        when(supplierRepo.findById("s1")).thenReturn(Optional.empty());
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var po = svc.createPO("t1", "s1", null, null, 0, null);

        assertEquals(BigDecimal.ZERO, po.getTotalAmount());
        assertEquals(1.0, po.getFxRate());
        assertEquals("USD", po.getFxCurrencyCode());
    }

    @Test
    void createPO_withFxRate_storesFx() {
        when(supplierRepo.findById("s1")).thenReturn(Optional.empty());
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var po = svc.createPO("t1", "s1", List.of(), null, 385.0, "AMD");

        assertEquals(385.0, po.getFxRate());
        assertEquals("AMD", po.getFxCurrencyCode());
    }

    // ── approvePO ────────────────────────────────────────────────────────────

    @Test
    void approvePO_fromPendingApproval_succeeds() {
        var po = PurchaseOrderEntity.builder().poId("po1").tenantId("t1")
                .status(PurchaseOrderEntity.PoStatus.PENDING_APPROVAL).build();
        when(poRepo.findByTenantIdAndPoId("t1", "po1")).thenReturn(Optional.of(po));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.approvePO("t1", "po1", "admin");

        assertEquals(PurchaseOrderEntity.PoStatus.APPROVED, result.getStatus());
        assertEquals("admin", result.getApprovedBy());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    void approvePO_fromDraft_succeeds() {
        var po = PurchaseOrderEntity.builder().poId("po1").tenantId("t1")
                .status(PurchaseOrderEntity.PoStatus.DRAFT).build();
        when(poRepo.findByTenantIdAndPoId("t1", "po1")).thenReturn(Optional.of(po));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.approvePO("t1", "po1", "admin");

        assertEquals(PurchaseOrderEntity.PoStatus.APPROVED, result.getStatus());
    }

    @Test
    void approvePO_fromApproved_throws() {
        var po = PurchaseOrderEntity.builder().poId("po1").tenantId("t1")
                .status(PurchaseOrderEntity.PoStatus.APPROVED).build();
        when(poRepo.findByTenantIdAndPoId("t1", "po1")).thenReturn(Optional.of(po));

        assertThrows(IllegalStateException.class,
                () -> svc.approvePO("t1", "po1", "admin"));
    }

    @Test
    void approvePO_notFound_throws() {
        when(poRepo.findByTenantIdAndPoId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.approvePO("t1", "bad", "admin"));
    }

    // ── matchDelivery ────────────────────────────────────────────────────────

    @Test
    void matchDelivery_exactMatch_noDiscrepancy() {
        var po = PurchaseOrderEntity.builder().poId("po1").tenantId("t1")
                .supplierId("s1").status(PurchaseOrderEntity.PoStatus.APPROVED).build();
        when(poRepo.findByTenantIdAndPoId("t1", "po1")).thenReturn(Optional.of(po));
        when(lineRepo.findByPoId("po1")).thenReturn(List.of(
                PurchaseOrderLineEntity.builder().lineId("l1").poId("po1").tenantId("t1")
                        .ingredientId("flour").qty(10).build()));
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryLineRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var received = List.of(
                new PurchaseOrderService.DeliveryLineInput("flour", "Flour", 10, "KG", new BigDecimal("2.50")));

        var delivery = svc.matchDelivery("t1", "po1", received, "all good");

        assertFalse(delivery.isHasDiscrepancy());
        assertEquals(SupplierDeliveryEntity.DeliveryStatus.MATCHED, delivery.getStatus());
        assertEquals(PurchaseOrderEntity.PoStatus.RECEIVED, po.getStatus());
    }

    @Test
    void matchDelivery_qtyMismatch_hasDiscrepancy() {
        var po = PurchaseOrderEntity.builder().poId("po1").tenantId("t1")
                .supplierId("s1").status(PurchaseOrderEntity.PoStatus.APPROVED).build();
        when(poRepo.findByTenantIdAndPoId("t1", "po1")).thenReturn(Optional.of(po));
        when(lineRepo.findByPoId("po1")).thenReturn(List.of(
                PurchaseOrderLineEntity.builder().lineId("l1").poId("po1").tenantId("t1")
                        .ingredientId("flour").qty(10).build()));
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryLineRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var received = List.of(
                new PurchaseOrderService.DeliveryLineInput("flour", "Flour", 8, "KG", new BigDecimal("2.50")));

        var delivery = svc.matchDelivery("t1", "po1", received, null);

        assertTrue(delivery.isHasDiscrepancy());
        assertEquals(SupplierDeliveryEntity.DeliveryStatus.DISCREPANCY, delivery.getStatus());
    }

    // ── findSuppliersForIngredient ───────────────────────────────────────────

    @Test
    void findSuppliersForIngredient_preferredFirst_thenByPrice() {
        var preferred = SupplierCatalogItemEntity.builder()
                .itemId("c1").tenantId("t1").supplierId("s1").ingredientId("flour")
                .unitPrice(new BigDecimal("3.00")).preferred(true).build();
        var cheap = SupplierCatalogItemEntity.builder()
                .itemId("c2").tenantId("t1").supplierId("s2").ingredientId("flour")
                .unitPrice(new BigDecimal("2.00")).preferred(false).build();

        when(catalogItemRepo.findByTenantIdAndIngredientId("t1", "flour"))
                .thenReturn(new java.util.ArrayList<>(List.of(cheap, preferred)));

        var result = svc.findSuppliersForIngredient("t1", "flour");

        assertEquals("s1", result.get(0).getSupplierId()); // preferred first
        assertEquals("s2", result.get(1).getSupplierId());
    }

    // ── getPO ────────────────────────────────────────────────────────────────

    @Test
    void getPO_notFound_throws() {
        when(poRepo.findByTenantIdAndPoId("t1", "x")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> svc.getPO("t1", "x"));
    }
}
