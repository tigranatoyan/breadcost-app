package com.breadcost.unit.service;

import com.breadcost.finance.FinanceService;
import com.breadcost.reporting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportKpiBlockRepository kpiBlockRepo;
    @Mock private CustomReportRepository customReportRepo;
    @Mock private FinanceService financeService;
    @InjectMocks private ReportService svc;

    // ── seedKpiBlocks ────────────────────────────────────────────────────────

    @Test
    void seedKpiBlocks_alreadySeeded_skips() {
        when(kpiBlockRepo.count()).thenReturn(5L);

        svc.seedKpiBlocks();

        verify(kpiBlockRepo, never()).saveAll(any());
    }

    @Test
    void seedKpiBlocks_empty_seeds15Blocks() {
        when(kpiBlockRepo.count()).thenReturn(0L);
        when(kpiBlockRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.seedKpiBlocks();

        verify(kpiBlockRepo).saveAll(argThat(list ->
                ((List<?>) list).size() == 15));
    }

    // ── addKpiBlock ──────────────────────────────────────────────────────────

    @Test
    void addKpiBlock_savesNewBlock() {
        when(kpiBlockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var block = svc.addKpiBlock("test_kpi", "Test KPI", "desc",
                "FINANCIAL", "REVENUE", "GBP");

        assertEquals("test_kpi", block.getBlockKey());
        assertEquals("Test KPI", block.getName());
        assertTrue(block.isActive());
    }

    // ── createReport ─────────────────────────────────────────────────────────

    @Test
    void createReport_setsBlocksInOrder() {
        when(customReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var report = svc.createReport("t1", "Daily Report", "Daily KPIs",
                "admin", List.of("total_revenue", "gross_margin_pct"));

        assertEquals("t1", report.getTenantId());
        assertEquals("Daily Report", report.getName());
        assertEquals(2, report.getBlocks().size());
        assertEquals(0, report.getBlocks().get(0).getDisplayOrder());
        assertEquals(1, report.getBlocks().get(1).getDisplayOrder());
    }

    // ── updateReport ─────────────────────────────────────────────────────────

    @Test
    void updateReport_replacesBlocks() {
        var existing = CustomReportEntity.builder()
                .reportId("r1").tenantId("t1").name("Old")
                .blocks(new ArrayList<>(List.of(
                        CustomReportBlockEntity.builder().id("b1").blockKey("total_revenue").displayOrder(0).build())))
                .build();
        when(customReportRepo.findByTenantIdAndReportId("t1", "r1"))
                .thenReturn(Optional.of(existing));
        when(customReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateReport("t1", "r1", "New Name",
                List.of("gross_margin_pct", "avg_order_value"));

        assertEquals("New Name", result.getName());
        assertEquals(2, result.getBlocks().size());
        assertEquals("gross_margin_pct", result.getBlocks().get(0).getBlockKey());
    }

    // ── deleteReport ─────────────────────────────────────────────────────────

    @Test
    void deleteReport_softDeletes() {
        var report = CustomReportEntity.builder()
                .reportId("r1").tenantId("t1").active(true).build();
        when(customReportRepo.findByTenantIdAndReportId("t1", "r1"))
                .thenReturn(Optional.of(report));
        when(customReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.deleteReport("t1", "r1");

        assertFalse(report.isActive());
    }

    // ── getReport ────────────────────────────────────────────────────────────

    @Test
    void getReport_notFound_throws() {
        when(customReportRepo.findByTenantIdAndReportId("t1", "bad"))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> svc.getReport("t1", "bad"));
    }

    // ── computeKpi ───────────────────────────────────────────────────────────

    @Test
    void computeKpi_totalRevenue_delegatesToFinanceService() {
        var block = ReportKpiBlockEntity.builder()
                .blockId("b1").blockKey("total_revenue").name("Total Revenue").unit("GBP").build();
        when(kpiBlockRepo.findByBlockKey("total_revenue")).thenReturn(Optional.of(block));
        when(financeService.totalRevenue("t1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .thenReturn(new BigDecimal("5000"));

        var result = svc.computeKpi("t1", "total_revenue",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertEquals("total_revenue", result.get("blockKey"));
        assertEquals(new BigDecimal("5000"), result.get("value"));
        assertEquals("GBP", result.get("unit"));
    }

    @Test
    void computeKpi_unknownBlock_throws() {
        when(kpiBlockRepo.findByBlockKey("nonexistent")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> svc.computeKpi("t1", "nonexistent", null, null));
    }

    @Test
    void computeKpi_unknownBlockKey_returnsZero() {
        var block = ReportKpiBlockEntity.builder()
                .blockId("b1").blockKey("custom_unknown").name("Custom").unit("x").build();
        when(kpiBlockRepo.findByBlockKey("custom_unknown")).thenReturn(Optional.of(block));

        var result = svc.computeKpi("t1", "custom_unknown", null, null);

        assertEquals(BigDecimal.ZERO, result.get("value"));
    }

    // ── runReport ────────────────────────────────────────────────────────────

    @Test
    void runReport_computesAllBlocks() {
        var report = CustomReportEntity.builder()
                .reportId("r1").tenantId("t1")
                .blocks(List.of(
                        CustomReportBlockEntity.builder().id("b1").blockKey("total_revenue").displayOrder(0).build(),
                        CustomReportBlockEntity.builder().id("b2").blockKey("gross_margin_pct").displayOrder(1).build()))
                .build();
        when(customReportRepo.findByTenantIdAndReportId("t1", "r1"))
                .thenReturn(Optional.of(report));

        var revenueBlock = ReportKpiBlockEntity.builder()
                .blockId("k1").blockKey("total_revenue").name("Revenue").unit("GBP").build();
        var marginBlock = ReportKpiBlockEntity.builder()
                .blockId("k2").blockKey("gross_margin_pct").name("Gross Margin").unit("%").build();
        when(kpiBlockRepo.findByBlockKey("total_revenue")).thenReturn(Optional.of(revenueBlock));
        when(kpiBlockRepo.findByBlockKey("gross_margin_pct")).thenReturn(Optional.of(marginBlock));
        when(financeService.totalRevenue(any(), any(), any())).thenReturn(new BigDecimal("1000"));
        when(financeService.grossMarginPct(any(), any(), any())).thenReturn(new BigDecimal("40"));

        var results = svc.runReport("t1", "r1", LocalDate.now(), LocalDate.now());

        assertEquals(2, results.size());
    }

    @Test
    void runReport_missingBlock_returnsError() {
        var report = CustomReportEntity.builder()
                .reportId("r1").tenantId("t1")
                .blocks(List.of(
                        CustomReportBlockEntity.builder().id("b1").blockKey("nonexistent").displayOrder(0).build()))
                .build();
        when(customReportRepo.findByTenantIdAndReportId("t1", "r1"))
                .thenReturn(Optional.of(report));
        when(kpiBlockRepo.findByBlockKey("nonexistent")).thenReturn(Optional.empty());

        var results = svc.runReport("t1", "r1", null, null);

        assertEquals(1, results.size());
        assertEquals("KPI block not found", results.get(0).get("error"));
    }
}
