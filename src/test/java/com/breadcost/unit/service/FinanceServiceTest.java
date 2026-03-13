package com.breadcost.unit.service;

import com.breadcost.finance.FinanceService;
import com.breadcost.invoice.InvoiceEntity;
import com.breadcost.invoice.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private InvoiceRepository invoiceRepo;
    @InjectMocks private FinanceService svc;

    // ── isEligibleForFGAdjustment ────────────────────────────────────────────

    @Test
    void fgEligible_issueToBatch_afterCutoff_true() {
        assertTrue(svc.isEligibleForFGAdjustment("IssueToBatch", 200L, 100L));
    }

    @Test
    void fgEligible_applyOverhead_afterCutoff_true() {
        assertTrue(svc.isEligibleForFGAdjustment("ApplyOverhead", 200L, 100L));
    }

    @Test
    void fgEligible_recordLabor_afterCutoff_true() {
        assertTrue(svc.isEligibleForFGAdjustment("RecordLabor", 200L, 100L));
    }

    @Test
    void fgEligible_beforeCutoff_false() {
        assertFalse(svc.isEligibleForFGAdjustment("IssueToBatch", 50L, 100L));
    }

    @Test
    void fgEligible_atCutoff_false() {
        assertFalse(svc.isEligibleForFGAdjustment("IssueToBatch", 100L, 100L));
    }

    @Test
    void fgEligible_unknownEventType_false() {
        assertFalse(svc.isEligibleForFGAdjustment("TransferInventory", 200L, 100L));
    }

    // ── calculateUnitCost ────────────────────────────────────────────────────

    @Test
    void unitCost_normalDivision() {
        assertEquals(new BigDecimal("2.5000"),
                svc.calculateUnitCost(new BigDecimal("250"), new BigDecimal("100")));
    }

    @Test
    void unitCost_zeroQty_returnsZero() {
        assertEquals(BigDecimal.ZERO,
                svc.calculateUnitCost(new BigDecimal("100"), BigDecimal.ZERO));
    }

    @Test
    void unitCost_roundsHalfUp() {
        // 10 / 3 = 3.3333...
        assertEquals(new BigDecimal("3.3333"),
                svc.calculateUnitCost(BigDecimal.TEN, new BigDecimal("3")));
    }

    // ── applyPostingRule ─────────────────────────────────────────────────────

    @Test
    void postingRule_receiveLot() {
        var result = svc.applyPostingRule("ReceiveLot");
        assertEquals("INV_RM", result.debitAccount);
        assertEquals("AP", result.creditAccount);
        assertFalse(result.operational);
    }

    @Test
    void postingRule_issueToBatch() {
        var result = svc.applyPostingRule("IssueToBatch");
        assertEquals("WIP_MATERIAL", result.debitAccount);
        assertEquals("INV_RM", result.creditAccount);
    }

    @Test
    void postingRule_recognizeProduction() {
        var result = svc.applyPostingRule("RecognizeProduction");
        assertEquals("INV_FG", result.debitAccount);
        assertEquals("WIP_TOTAL", result.creditAccount);
    }

    @Test
    void postingRule_transfer_operational() {
        var result = svc.applyPostingRule("TransferInventory");
        assertTrue(result.operational);
        assertNull(result.debitAccount);
    }

    @Test
    void postingRule_unknown_emptyResult() {
        var result = svc.applyPostingRule("SomethingElse");
        assertNull(result.debitAccount);
        assertNull(result.creditAccount);
        assertFalse(result.operational);
    }

    // ── totalRevenue ─────────────────────────────────────────────────────────

    @Test
    void totalRevenue_onlyPaidAndIssued() {
        var inv1 = invoice("PAID", "500.00", LocalDate.of(2026, 1, 15));
        var inv2 = invoice("ISSUED", "300.00", LocalDate.of(2026, 1, 20));
        var inv3 = invoice("DRAFT", "200.00", LocalDate.of(2026, 1, 10));
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of(inv1, inv2, inv3));

        assertEquals(new BigDecimal("800.00"),
                svc.totalRevenue("t1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }

    @Test
    void totalRevenue_dateFiltering() {
        var inv = invoice("PAID", "100.00", LocalDate.of(2026, 3, 1));
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of(inv));

        assertEquals(BigDecimal.ZERO,
                svc.totalRevenue("t1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }

    // ── grossMarginPct ───────────────────────────────────────────────────────

    @Test
    void grossMargin_40percent() {
        var inv = invoice("PAID", "1000.00", LocalDate.of(2026, 1, 15));
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of(inv));

        // COGS = 60% of 1000 = 600; margin = (1000-600)/1000 = 40%
        assertEquals(new BigDecimal("40.00"),
                svc.grossMarginPct("t1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }

    @Test
    void grossMargin_zeroRevenue_returnsZero() {
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of());

        assertEquals(BigDecimal.ZERO,
                svc.grossMarginPct("t1", null, null));
    }

    // ── avgOrderValue ────────────────────────────────────────────────────────

    @Test
    void avgOrderValue_calculation() {
        var inv1 = invoice("PAID", "100.00", LocalDate.of(2026, 1, 10));
        var inv2 = invoice("ISSUED", "200.00", LocalDate.of(2026, 1, 15));
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of(inv1, inv2));

        assertEquals(new BigDecimal("150.00"),
                svc.avgOrderValue("t1", null, null));
    }

    @Test
    void avgOrderValue_noOrders_returnsZero() {
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of());

        assertEquals(BigDecimal.ZERO, svc.avgOrderValue("t1", null, null));
    }

    // ── outstandingInvoices ──────────────────────────────────────────────────

    @Test
    void outstandingInvoices_draftAndIssued() {
        var inv1 = invoice("DRAFT", "100.00", null);
        var inv2 = invoice("ISSUED", "200.00", LocalDate.now());
        var inv3 = invoice("PAID", "500.00", LocalDate.now());
        when(invoiceRepo.findByTenantId("t1")).thenReturn(List.of(inv1, inv2, inv3));

        assertEquals(new BigDecimal("300.00"), svc.outstandingInvoices("t1"));
    }

    // ── overdueInvoiceCount ──────────────────────────────────────────────────

    @Test
    void overdueCount_delegates() {
        var ov1 = invoice("OVERDUE", "100.00", LocalDate.now());
        when(invoiceRepo.findByTenantIdAndStatus("t1", InvoiceEntity.InvoiceStatus.OVERDUE))
                .thenReturn(List.of(ov1));

        assertEquals(1L, svc.overdueInvoiceCount("t1"));
    }

    // ── placeholders ─────────────────────────────────────────────────────────

    @Test
    void deliveryRate_returnsPlaceholder() {
        assertEquals(new BigDecimal("95.00"), svc.deliveryCompletionRate("t1", null, null));
    }

    @Test
    void stockTurnover_returnsPlaceholder() {
        assertEquals(new BigDecimal("4.50"), svc.stockTurnover("t1", null, null));
    }

    @Test
    void productionEfficiency_returnsPlaceholder() {
        assertEquals(new BigDecimal("92.00"), svc.productionEfficiency("t1", null, null));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InvoiceEntity invoice(String status, String amount, LocalDate issuedDate) {
        InvoiceEntity inv = new InvoiceEntity();
        inv.setStatus(InvoiceEntity.InvoiceStatus.valueOf(status));
        inv.setTotalAmount(new BigDecimal(amount));
        inv.setIssuedDate(issuedDate);
        return inv;
    }
}
