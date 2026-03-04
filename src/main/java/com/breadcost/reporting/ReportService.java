package com.breadcost.reporting;

import com.breadcost.finance.FinanceService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Report management service.
 * BC-E16: KPI catalog, custom reports, financial KPI computation, Excel/PDF export.
 */
@Service
@Transactional
public class ReportService {

    private final ReportKpiBlockRepository kpiBlockRepo;
    private final CustomReportRepository customReportRepo;
    private final FinanceService financeService;

    public ReportService(ReportKpiBlockRepository kpiBlockRepo,
                         CustomReportRepository customReportRepo,
                         FinanceService financeService) {
        this.kpiBlockRepo = kpiBlockRepo;
        this.customReportRepo = customReportRepo;
        this.financeService = financeService;
    }

    // ── BC-1601: KPI Block Catalog ────────────────────────────────────────────

    /**
     * Seed the standard KPI block catalog if empty.
     */
    public void seedKpiBlocks() {
        if (kpiBlockRepo.count() > 0) return;
        kpiBlockRepo.saveAll(List.of(
                kpi("total_revenue", "Total Revenue", "Sum of all invoiced revenue",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.REVENUE, "GBP"),
                kpi("cost_of_goods_sold", "Cost of Goods Sold", "Sum of ingredient + production costs",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.AGGREGATE, "GBP"),
                kpi("gross_margin_pct", "Gross Margin %", "( Revenue - COGS ) / Revenue × 100",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.RATIO, "%"),
                kpi("total_orders", "Total Orders", "Number of completed orders",
                        ReportKpiBlockEntity.KpiCategory.CUSTOMER, ReportKpiBlockEntity.QueryType.COUNT, "count"),
                kpi("avg_order_value", "Average Order Value", "Revenue / completed orders",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.RATIO, "GBP"),
                kpi("delivery_completion_rate", "Delivery Completion Rate",
                        "Completed deliveries / total deliveries × 100",
                        ReportKpiBlockEntity.KpiCategory.DELIVERY, ReportKpiBlockEntity.QueryType.RATIO, "%"),
                kpi("stock_turnover", "Stock Turnover", "COGS / avg inventory value",
                        ReportKpiBlockEntity.KpiCategory.INVENTORY, ReportKpiBlockEntity.QueryType.RATIO, "x"),
                kpi("production_efficiency", "Production Efficiency",
                        "Actual output / planned output × 100",
                        ReportKpiBlockEntity.KpiCategory.PRODUCTION, ReportKpiBlockEntity.QueryType.RATIO, "%"),
                kpi("outstanding_invoices", "Outstanding Invoices",
                        "Sum of unpaid invoice amounts",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.AGGREGATE, "GBP"),
                kpi("overdue_invoices", "Overdue Invoices",
                        "Count of past-due unpaid invoices",
                        ReportKpiBlockEntity.KpiCategory.FINANCIAL, ReportKpiBlockEntity.QueryType.COUNT, "count")
        ));
    }

    @Transactional(readOnly = true)
    public List<ReportKpiBlockEntity> listKpiBlocks() {
        return kpiBlockRepo.findByActive(true);
    }

    @Transactional(readOnly = true)
    public List<ReportKpiBlockEntity> listAllKpiBlocks() {
        return kpiBlockRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReportKpiBlockEntity> listKpiBlocksByCategory(String category) {
        return kpiBlockRepo.findByCategory(
                ReportKpiBlockEntity.KpiCategory.valueOf(category.toUpperCase()));
    }

    public ReportKpiBlockEntity addKpiBlock(String blockKey, String name, String description,
                                             String category, String queryType, String unit) {
        ReportKpiBlockEntity block = ReportKpiBlockEntity.builder()
                .blockId(UUID.randomUUID().toString())
                .blockKey(blockKey)
                .name(name)
                .description(description)
                .category(ReportKpiBlockEntity.KpiCategory.valueOf(category.toUpperCase()))
                .queryType(ReportKpiBlockEntity.QueryType.valueOf(queryType.toUpperCase()))
                .unit(unit)
                .active(true)
                .build();
        return kpiBlockRepo.save(block);
    }

    // ── BC-1602: Custom Report Builder ────────────────────────────────────────

    /**
     * Create a custom report with a selection of KPI blocks.
     *
     * @param blockKeys ordered list of KPI block keys to include
     */
    public CustomReportEntity createReport(String tenantId, String name, String description,
                                            String createdBy, List<String> blockKeys) {
        List<CustomReportBlockEntity> blocks = new ArrayList<>();
        for (int i = 0; i < blockKeys.size(); i++) {
            blocks.add(CustomReportBlockEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .blockKey(blockKeys.get(i))
                    .displayOrder(i)
                    .build());
        }
        CustomReportEntity report = CustomReportEntity.builder()
                .reportId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .createdBy(createdBy)
                .active(true)
                .blocks(blocks)
                .build();
        return customReportRepo.save(report);
    }

    public CustomReportEntity updateReport(String tenantId, String reportId, String name,
                                            List<String> blockKeys) {
        CustomReportEntity report = getReport(tenantId, reportId);
        report.setName(name);
        report.getBlocks().clear();
        for (int i = 0; i < blockKeys.size(); i++) {
            report.getBlocks().add(CustomReportBlockEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .blockKey(blockKeys.get(i))
                    .displayOrder(i)
                    .build());
        }
        return customReportRepo.save(report);
    }

    public void deleteReport(String tenantId, String reportId) {
        CustomReportEntity report = getReport(tenantId, reportId);
        report.setActive(false);
        customReportRepo.save(report);
    }

    @Transactional(readOnly = true)
    public List<CustomReportEntity> listReports(String tenantId) {
        return customReportRepo.findByTenantIdAndActive(tenantId, true);
    }

    @Transactional(readOnly = true)
    public CustomReportEntity getReport(String tenantId, String reportId) {
        return customReportRepo.findByTenantIdAndReportId(tenantId, reportId)
                .orElseThrow(() -> new NoSuchElementException("Report not found: " + reportId));
    }

    // ── BC-1603: Advanced financial KPI computation ────────────────────────────

    /**
     * Compute value for a KPI block over a date range.
     * Returns a Map with: blockKey, value, unit, dateFrom, dateTo
     * BC-1603
     */
    @Transactional(readOnly = true)
    public Map<String, Object> computeKpi(String tenantId, String blockKey,
                                           LocalDate dateFrom, LocalDate dateTo) {
        ReportKpiBlockEntity block = kpiBlockRepo.findByBlockKey(blockKey)
                .orElseThrow(() -> new NoSuchElementException("KPI block not found: " + blockKey));

        Object value = computeKpiValue(tenantId, blockKey, dateFrom, dateTo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blockKey", blockKey);
        result.put("name", block.getName());
        result.put("value", value);
        result.put("unit", block.getUnit());
        result.put("dateFrom", dateFrom != null ? dateFrom.toString() : "");
        result.put("dateTo", dateTo != null ? dateTo.toString() : "");
        return result;
    }

    /**
     * Run all blocks in a custom report and return computed values.
     * BC-1603
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> runReport(String tenantId, String reportId,
                                                LocalDate dateFrom, LocalDate dateTo) {
        CustomReportEntity report = getReport(tenantId, reportId);
        List<Map<String, Object>> results = new ArrayList<>();
        for (CustomReportBlockEntity block : report.getBlocks()) {
            try {
                results.add(computeKpi(tenantId, block.getBlockKey(), dateFrom, dateTo));
            } catch (NoSuchElementException e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("blockKey", block.getBlockKey());
                err.put("error", "KPI block not found");
                results.add(err);
            }
        }
        return results;
    }

    // ── BC-1604: Report export to Excel ──────────────────────────────────────

    /**
     * Export report results to Excel (XLSX).
     * BC-1604
     */
    public byte[] exportReportToExcel(String tenantId, String reportId,
                                       LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> data = runReport(tenantId, reportId, dateFrom, dateTo);
        CustomReportEntity report = getReport(tenantId, reportId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Report");

            // Header row
            Row header = sheet.createRow(0);
            String[] cols = {"KPI Block", "Name", "Value", "Unit", "Date From", "Date To"};
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Map<String, Object> row : data) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(str(row.get("blockKey")));
                r.createCell(1).setCellValue(str(row.get("name")));
                Object val = row.get("value");
                if (val instanceof Number) {
                    r.createCell(2).setCellValue(((Number) val).doubleValue());
                } else {
                    r.createCell(2).setCellValue(str(val));
                }
                r.createCell(3).setCellValue(str(row.get("unit")));
                r.createCell(4).setCellValue(str(row.get("dateFrom")));
                r.createCell(5).setCellValue(str(row.get("dateTo")));
            }

            // Auto-size columns
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            // Report metadata on second sheet
            Sheet meta = wb.createSheet("Metadata");
            meta.createRow(0).createCell(0).setCellValue("Report: " + report.getName());
            meta.createRow(1).createCell(0).setCellValue("Tenant: " + tenantId);
            meta.createRow(2).createCell(0).setCellValue("Period: " + dateFrom + " to " + dateTo);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate Excel report", e);
        }
    }

    /**
     * Export report results as a simple CSV-like byte array (simulates PDF for testing).
     * BC-1604 — actual PDF would require iText/Apache PDFBox dependency.
     */
    public byte[] exportReportToCsv(String tenantId, String reportId,
                                     LocalDate dateFrom, LocalDate dateTo) {
        List<Map<String, Object>> data = runReport(tenantId, reportId, dateFrom, dateTo);
        StringBuilder sb = new StringBuilder("KPI Block,Name,Value,Unit,Date From,Date To\n");
        for (Map<String, Object> row : data) {
            sb.append(str(row.get("blockKey"))).append(",")
                    .append(str(row.get("name"))).append(",")
                    .append(str(row.get("value"))).append(",")
                    .append(str(row.get("unit"))).append(",")
                    .append(str(row.get("dateFrom"))).append(",")
                    .append(str(row.get("dateTo"))).append("\n");
        }
        return sb.toString().getBytes();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Object computeKpiValue(String tenantId, String blockKey,
                                    LocalDate dateFrom, LocalDate dateTo) {
        // Delegate to FinanceService for real DB queries where available;
        // fall back to computed stubs for unimplemented KPIs.
        return switch (blockKey) {
            case "total_revenue" -> financeService.totalRevenue(tenantId, dateFrom, dateTo);
            case "cost_of_goods_sold" -> financeService.totalCostOfGoods(tenantId, dateFrom, dateTo);
            case "gross_margin_pct" -> financeService.grossMarginPct(tenantId, dateFrom, dateTo);
            case "total_orders" -> financeService.totalOrderCount(tenantId, dateFrom, dateTo);
            case "avg_order_value" -> financeService.avgOrderValue(tenantId, dateFrom, dateTo);
            case "delivery_completion_rate" -> financeService.deliveryCompletionRate(tenantId, dateFrom, dateTo);
            case "stock_turnover" -> financeService.stockTurnover(tenantId, dateFrom, dateTo);
            case "production_efficiency" -> financeService.productionEfficiency(tenantId, dateFrom, dateTo);
            case "outstanding_invoices" -> financeService.outstandingInvoices(tenantId);
            case "overdue_invoices" -> financeService.overdueInvoiceCount(tenantId);
            default -> BigDecimal.ZERO;
        };
    }

    private ReportKpiBlockEntity kpi(String key, String name, String desc,
                                      ReportKpiBlockEntity.KpiCategory cat,
                                      ReportKpiBlockEntity.QueryType qt, String unit) {
        return ReportKpiBlockEntity.builder()
                .blockId(UUID.randomUUID().toString())
                .blockKey(key).name(name).description(desc)
                .category(cat).queryType(qt).unit(unit).active(true)
                .build();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
