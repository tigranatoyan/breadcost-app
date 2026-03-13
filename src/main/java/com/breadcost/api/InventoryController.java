package com.breadcost.api;

import com.breadcost.commands.*;
import com.breadcost.masterdata.ItemEntity;
import com.breadcost.masterdata.ItemRepository;
import com.breadcost.masterdata.StockAlertService;
import com.breadcost.projections.InventoryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for inventory operations — FR-5.1 through FR-5.11
 */
@RestController
@RequestMapping("/v1/inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {
    private final ReceiveLotCommandHandler receiveLotHandler;
    private final TransferInventoryCommandHandler transferInventoryHandler;
    private final InventoryProjection inventoryProjection;
    private final ItemRepository itemRepository;
    private final StockAlertService stockAlertService;

    // ─── POSITIONS ───────────────────────────────────────────────────────────

    @GetMapping("/positions")
    @PreAuthorize("hasAnyRole('Admin','Manager','ProductionUser','FinanceUser','Viewer','Warehouse')")
    public ResponseEntity<List<InventoryProjection.InventoryPosition>> getPositions(
            @RequestParam String tenantId,
            @RequestParam(required = false) String siteId) {

        List<InventoryProjection.InventoryPosition> all = siteId != null
                ? inventoryProjection.getPositionsBySite(siteId)
                : inventoryProjection.getAllPositions();

        List<InventoryProjection.InventoryPosition> filtered = all.stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .toList();

        return ResponseEntity.ok(filtered);
    }

    // ─── RECEIVE ─────────────────────────────────────────────────────────────

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('Admin','Warehouse')")
    public ResponseEntity<CommandResult> receiveLot(@Valid @RequestBody ReceiveLotCommand command) {
        log.info("API: ReceiveLot - receiptId={}", command.getReceiptId());
        CommandResult result = receiveLotHandler.handle(command);
        return ResponseEntity.ok(result);
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('Admin','Warehouse','ProductionUser')")
    public ResponseEntity<CommandResult> transferInventory(@Valid @RequestBody TransferInventoryCommand command) {
        log.info("API: TransferInventory - itemId={}", command.getItemId());
        CommandResult result = transferInventoryHandler.handle(command);
        return ResponseEntity.ok(result);
    }

    // ─── ADJUST ──────────────────────────────────────────────────────────────

    @Data
    public static class AdjustRequest {
        @NotBlank private String tenantId;
        private String siteId;
        @NotBlank private String itemId;
        /** Positive = add, negative = reduce */
        @NotNull private BigDecimal adjustmentQty;
        private String unit;
        /** WASTE / SPOILAGE / COUNT_CORRECTION / OTHER */
        @NotBlank private String reasonCode;
        private String notes;
    }

    @Data
    public static class AdjustResponse {
        private String message;
        private String itemId;
        private BigDecimal adjustmentQty;
        private String reasonCode;
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('Admin','Warehouse')")
    public ResponseEntity<AdjustResponse> adjustInventory(@Valid @RequestBody AdjustRequest req) {
        log.info("Inventory adjustment: item={}, qty={}, reason={}", req.getItemId(), req.getAdjustmentQty(), req.getReasonCode());
        // Apply adjustment via the in-memory projection directly
        inventoryProjection.applyAdjustment(req.getTenantId(), req.getSiteId(), req.getItemId(),
                req.getAdjustmentQty(), req.getReasonCode());

        AdjustResponse resp = new AdjustResponse();
        resp.setMessage("Adjustment applied");
        resp.setItemId(req.getItemId());
        resp.setAdjustmentQty(req.getAdjustmentQty());
        resp.setReasonCode(req.getReasonCode());
        return ResponseEntity.ok(resp);
    }

    // ─── ALERTS ──────────────────────────────────────────────────────────────

    @Data
    static class StockAlert {
        String itemId;
        String itemName;
        String tenantId;
        String siteId;
        BigDecimal onHandQty;
        double minThreshold;
        String uom;
        String severity; // LOW / CRITICAL
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('Admin','Manager','ProductionUser','FinanceUser','Viewer','Warehouse')")
    public ResponseEntity<List<StockAlert>> getAlerts(@RequestParam String tenantId) {
        List<ItemEntity> items = itemRepository.findByTenantId(tenantId).stream()
                .filter(i -> i.getMinStockThreshold() > 0)
                .toList();

        // Aggregate on-hand by itemId across all positions for this tenant
        Map<String, BigDecimal> onHandByItem = inventoryProjection.getAllPositions().stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .collect(Collectors.groupingBy(
                        InventoryProjection.InventoryPosition::getItemId,
                        Collectors.reducing(BigDecimal.ZERO, InventoryProjection.InventoryPosition::getOnHandQty, BigDecimal::add)
                ));

        List<StockAlert> alerts = new ArrayList<>();
        for (ItemEntity item : items) {
            BigDecimal onHand = onHandByItem.getOrDefault(item.getItemId(), BigDecimal.ZERO);
            if (onHand.doubleValue() < item.getMinStockThreshold()) {
                StockAlert alert = new StockAlert();
                alert.itemId = item.getItemId();
                alert.itemName = item.getName();
                alert.tenantId = tenantId;
                alert.siteId = null;
                alert.onHandQty = onHand;
                alert.minThreshold = item.getMinStockThreshold();
                alert.uom = item.getBaseUom();
                alert.severity = onHand.doubleValue() <= 0 ? "CRITICAL" : "LOW";
                alerts.add(alert);
            }
        }

        return ResponseEntity.ok(alerts);
    }

    // ─── LOW STOCK + AUTO PLAN ──────────────────────────────────────────────

    /**
     * G-6: Enhanced low-stock alerts with affected products.
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('Admin','Manager','Warehouse')")
    public ResponseEntity<List<StockAlertService.LowStockAlert>> getLowStock(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(stockAlertService.detectLowStock(tenantId));
    }

    /**
     * G-6: Auto-create a DRAFT production plan based on confirmed orders and stock levels.
     */
    @PostMapping("/auto-plan")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<Map<String, Object>> autoCreatePlan(
            @RequestParam String tenantId,
            @RequestParam(required = false) String siteId,
            org.springframework.security.core.Authentication auth) {
        String userId = auth != null ? auth.getName() : "system";
        var result = stockAlertService.autoCreateProductionPlan(tenantId, siteId, userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", result.message());
        response.put("warnings", result.warnings());
        if (result.plan() != null) {
            response.put("planId", result.plan().getPlanId());
            response.put("workOrderCount", result.plan().getWorkOrders().size());
        }
        return ResponseEntity.ok(response);
    }
}
