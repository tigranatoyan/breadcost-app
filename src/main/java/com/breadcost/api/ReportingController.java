package com.breadcost.api;

import com.breadcost.reporting.CustomReportEntity;
import com.breadcost.reporting.ReportKpiBlockEntity;
import com.breadcost.reporting.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Reporting REST controller.
 * BC-1601 – KPI block catalog
 * BC-1602 – Custom report builder
 * BC-1603 – Advanced financial KPIs
 * BC-1604 – Report export (Excel / CSV)
 */
@Tag(name = "Reporting", description = "KPI dashboards, revenue, and operational reports")
@RestController
@RequestMapping("/v2/reports")
@PreAuthorize("hasAnyRole('Admin','Manager','FinanceUser','Viewer')")
public class ReportingController {

    private final ReportService reportService;

    public ReportingController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }

    // ── BC-1601: KPI Block Catalog ────────────────────────────────────────────

    /** GET /v2/reports/kpi-blocks — list all active KPI blocks */
    @GetMapping("/kpi-blocks")
    public List<ReportKpiBlockEntity> listKpiBlocks(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return includeInactive ? reportService.listAllKpiBlocks() : reportService.listKpiBlocks();
    }

    /** GET /v2/reports/kpi-blocks/category/{category} — filter by category */
    @GetMapping("/kpi-blocks/category/{category}")
    public List<ReportKpiBlockEntity> listByCategory(@PathVariable String category) {
        return reportService.listKpiBlocksByCategory(category);
    }

    /** POST /v2/reports/kpi-blocks — add custom KPI block */
    @PostMapping("/kpi-blocks")
    public ResponseEntity<ReportKpiBlockEntity> addKpiBlock(@RequestBody Map<String, Object> body) {
        ReportKpiBlockEntity block = reportService.addKpiBlock(
                (String) body.get("blockKey"),
                (String) body.get("name"),
                (String) body.get("description"),
                (String) body.get("category"),
                (String) body.get("queryType"),
                (String) body.get("unit")
        );
        return ResponseEntity.status(201).body(block);
    }

    // ── BC-1602: Custom Report Builder ────────────────────────────────────────

    /** POST /v2/reports — create a custom report */
    @PostMapping
    public ResponseEntity<CustomReportEntity> createReport(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> blocks = (List<String>) body.getOrDefault("blockKeys", List.of());
        CustomReportEntity report = reportService.createReport(
                (String) body.get("tenantId"),
                (String) body.get("name"),
                (String) body.get("description"),
                (String) body.get("createdBy"),
                blocks
        );
        return ResponseEntity.status(201).body(report);
    }

    /** GET /v2/reports?tenantId=... — list tenant reports */
    @GetMapping
    public List<CustomReportEntity> listReports(@RequestParam String tenantId) {
        return reportService.listReports(tenantId);
    }

    /** GET /v2/reports/{id}?tenantId=... — get single report */
    @GetMapping("/{id}")
    public CustomReportEntity getReport(@PathVariable String id, @RequestParam String tenantId) {
        return reportService.getReport(tenantId, id);
    }

    /** DELETE /v2/reports/{id}?tenantId=... */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id, @RequestParam String tenantId) {
        reportService.deleteReport(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    /** PUT /v2/reports/{id}?tenantId=... — update a custom report */
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public CustomReportEntity updateReport(@PathVariable String id,
                                           @RequestParam String tenantId,
                                           @RequestBody Map<String, Object> body) {
        List<String> blocks = (List<String>) body.getOrDefault("blocks", List.of());
        return reportService.updateReport(tenantId, id,
                (String) body.get("name"), blocks);
    }

    // ── BC-1603: Run Report / Compute KPI ─────────────────────────────────────

    /** GET /v2/reports/{id}/run?tenantId=...&dateFrom=...&dateTo=... */
    @GetMapping("/{id}/run")
    public List<Map<String, Object>> runReport(
            @PathVariable String id,
            @RequestParam String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return reportService.runReport(tenantId, id, parseDate(dateFrom), parseDate(dateTo));
    }

    /** GET /v2/reports/kpi?blockKey=...&tenantId=...&dateFrom=...&dateTo=... */
    @GetMapping("/kpi")
    public Map<String, Object> computeKpi(
            @RequestParam String blockKey,
            @RequestParam String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return reportService.computeKpi(tenantId, blockKey, parseDate(dateFrom), parseDate(dateTo));
    }

    // ── BC-1604: Report Export ─────────────────────────────────────────────────

    /** GET /v2/reports/{id}/export?tenantId=...&dateFrom=...&dateTo=...&format=excel|csv */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String id,
            @RequestParam String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "excel") String format) {

        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = reportService.exportReportToCsv(tenantId, id, parseDate(dateFrom), parseDate(dateTo));
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=\"report-" + id + ".csv\"")
                    .body(csv);
        }

        byte[] excel = reportService.exportReportToExcel(tenantId, id, parseDate(dateFrom), parseDate(dateTo));
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"report-" + id + ".xlsx\"")
                .body(excel);
    }
}
