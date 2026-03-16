package com.breadcost.api;

import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for read model queries
 * Implements view endpoints from API_SURFACE.yaml
 */
@Tag(name = "Views", description = "Materialized view endpoints for dashboards")
@RestController
@RequestMapping("/v1/views")
@Slf4j
@RequiredArgsConstructor
public class ViewController {

    private static final String MESSAGE = "message";

    private final InventoryProjection inventoryProjection;

    /**
     * GET /v1/views/batch-cost/{batchId}
     * Query batch cost view
     */
    @GetMapping("/batch-cost/{batchId}")
    @PreAuthorize("hasAnyRole('Admin', 'Viewer', 'ProductionUser', 'FinanceUser')")
    public ResponseEntity<Map<String, Object>> getBatchCost(@PathVariable String batchId) {
        log.info("API: GetBatchCostView - batchId={}", batchId);
        // Projection would be queried here
        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "status", "PLACEHOLDER",
                MESSAGE, "BatchCostView projection not yet implemented"
        ));
    }

    /**
     * GET /v1/views/inventory-valuation
     * Query inventory valuation view
     */
    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAnyRole('Admin', 'Viewer', 'InventoryController', 'FinanceUser')")
    public ResponseEntity<List<InventoryProjection.InventoryPosition>> getInventoryValuation(
            @RequestParam(required = false) String siteId) {
        log.info("API: GetInventoryValuationView - siteId={}", siteId);
        List<InventoryProjection.InventoryPosition> positions = 
                siteId != null ? inventoryProjection.getPositionsBySite(siteId) 
                              : inventoryProjection.getAllPositions();
        return ResponseEntity.ok(positions);
    }

    /**
     * GET /v1/views/wip
     * Query WIP (Work in Progress) view
     */
    @GetMapping("/wip")
    @PreAuthorize("hasAnyRole('Admin', 'Viewer', 'FinanceUser')")
    public ResponseEntity<Map<String, Object>> getWIP(
            @RequestParam(required = false) String siteId) {
        log.info("API: GetWIPView - siteId={}", siteId);
        return ResponseEntity.ok(Map.of(
                "siteId", siteId != null ? siteId : "ALL",
                MESSAGE, "WIPView projection not yet implemented"
        ));
    }

    /**
     * GET /v1/views/cogs-bridge/{periodId}
     * Query COGS bridge report
     */
    @GetMapping("/cogs-bridge/{periodId}")
    @PreAuthorize("hasAnyRole('Admin', 'FinanceUser', 'FinanceAdmin')")
    public ResponseEntity<Map<String, Object>> getCOGSBridge(@PathVariable String periodId) {
        log.info("API: GetCOGSBridgeView - periodId={}", periodId);
        return ResponseEntity.ok(Map.of(
                "periodId", periodId,
                MESSAGE, "COGSBridgeView projection not yet implemented"
        ));
    }

    /**
     * GET /v1/views/exceptions
     * Query exception queue
     */
    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyRole('Admin', 'ProductionSupervisor', 'InventoryController', 'FinanceUser')")
    public ResponseEntity<Map<String, Object>> getExceptions(
            @RequestParam(required = false) String status) {
        log.info("API: GetExceptionQueueView - status={}", status);
        return ResponseEntity.ok(Map.of(
                "status", status != null ? status : "ALL",
                MESSAGE, "ExceptionQueueView projection not yet implemented"
        ));
    }
}
