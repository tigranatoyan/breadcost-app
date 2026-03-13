package com.breadcost.api;

import com.breadcost.purchaseorder.*;
import com.breadcost.supplier.SupplierCatalogItemEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Purchase order REST API — BC-E13
 *
 * POST /v2/purchase-orders/suggest             — BC-1302: suggest POs
 * POST /v2/purchase-orders                     — BC-1303: create PO
 * PUT  /v2/purchase-orders/{id}/approve        — BC-1303: approve PO
 * GET  /v2/purchase-orders?tenantId=...        — list POs
 * GET  /v2/purchase-orders/{id}?tenantId=...   — get PO detail + lines
 * GET  /v2/purchase-orders/{id}/export         — BC-1304: Excel export
 * POST /v2/purchase-orders/{id}/deliveries     — BC-1305: match delivery
 * GET  /v2/purchase-orders/{id}/deliveries     — list deliveries for PO
 */
@Tag(name = "Purchase Orders", description = "Supplier purchase orders, approvals, and delivery matching")
@RestController
@RequestMapping("/v2/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager','Warehouse')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // ── Request DTOs ──────────────────────────────────────────────────────────

    @Data
    public static class SuggestPORequest {
        @NotBlank private String tenantId;
    }

    @Data
    public static class CreatePORequest {
        @NotBlank private String tenantId;
        @NotBlank private String supplierId;
        private List<LineDto> lines;
        private String notes;
        private double fxRate = 1.0;
        private String fxCurrencyCode = "USD";

        @Data
        public static class LineDto {
            private String ingredientId;
            private String ingredientName;
            private double qty;
            private String unit;
            private BigDecimal unitPrice;
            private String currency;
        }
    }

    @Data
    public static class ApproveRequest {
        private String approvedBy;
    }

    @Data
    public static class MatchDeliveryRequest {
        @NotBlank private String tenantId;
        private List<DeliveryLineDto> lines;
        private String notes;

        @Data
        public static class DeliveryLineDto {
            private String ingredientId;
            private String ingredientName;
            private double qtyReceived;
            private String unit;
            private BigDecimal unitPrice;
        }
    }

    // ── Response type ─────────────────────────────────────────────────────────

    record PODetailResponse(PurchaseOrderEntity po, List<PurchaseOrderLineEntity> lines) {}

    // ── BC-1302: Suggest ──────────────────────────────────────────────────────

    @PostMapping("/suggest")
    public ResponseEntity<List<PurchaseOrderEntity>> suggestPOs(
            @Valid @RequestBody SuggestPORequest req) {
        return ResponseEntity.ok(purchaseOrderService.suggestPOs(req.getTenantId()));
    }

    // ── BC-1303: Create + Approve ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<PODetailResponse> createPO(
            @Valid @RequestBody CreatePORequest req) {
        List<PurchaseOrderService.LineInput> lines = req.getLines() == null
                ? List.of()
                : req.getLines().stream()
                    .map(l -> new PurchaseOrderService.LineInput(
                            l.getIngredientId(), l.getIngredientName(),
                            l.getQty(), l.getUnit(),
                            l.getUnitPrice(), l.getCurrency()))
                    .collect(Collectors.toList());

        PurchaseOrderEntity po = purchaseOrderService.createPO(
                req.getTenantId(), req.getSupplierId(), lines,
                req.getNotes(), req.getFxRate(), req.getFxCurrencyCode());

        List<PurchaseOrderLineEntity> poLines = purchaseOrderService.getPOLines(po.getPoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PODetailResponse(po, poLines));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderEntity> approvePO(
            @PathVariable("id") String poId,
            @RequestParam String tenantId,
            @RequestBody(required = false) ApproveRequest req) {
        String approvedBy = req != null ? req.getApprovedBy() : "system";
        return ResponseEntity.ok(purchaseOrderService.approvePO(tenantId, poId, approvedBy));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderEntity>> listPOs(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(purchaseOrderService.listPOs(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PODetailResponse> getPO(
            @PathVariable("id") String poId,
            @RequestParam String tenantId) {
        PurchaseOrderEntity po = purchaseOrderService.getPO(tenantId, poId);
        List<PurchaseOrderLineEntity> lines = purchaseOrderService.getPOLines(poId);
        return ResponseEntity.ok(new PODetailResponse(po, lines));
    }

    // ── BC-1304: Excel export ─────────────────────────────────────────────────

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportPO(
            @PathVariable("id") String poId,
            @RequestParam String tenantId) {
        byte[] bytes = purchaseOrderService.exportToExcel(tenantId, poId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"PO-" + poId.substring(0, 8) + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ── BC-1305: Delivery matching ────────────────────────────────────────────

    @PostMapping("/{id}/deliveries")
    public ResponseEntity<Object> matchDelivery(
            @PathVariable("id") String poId,
            @Valid @RequestBody MatchDeliveryRequest req) {
        List<PurchaseOrderService.DeliveryLineInput> lines = req.getLines() == null
                ? List.of()
                : req.getLines().stream()
                    .map(l -> new PurchaseOrderService.DeliveryLineInput(
                            l.getIngredientId(), l.getIngredientName(),
                            l.getQtyReceived(), l.getUnit(), l.getUnitPrice()))
                    .collect(Collectors.toList());

        SupplierDeliveryEntity delivery = purchaseOrderService.matchDelivery(
                req.getTenantId(), poId, lines, req.getNotes());

        List<SupplierDeliveryLineEntity> dlines = purchaseOrderService.getDeliveryLines(delivery.getDeliveryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                java.util.Map.of("delivery", delivery, "lines", dlines));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<List<SupplierDeliveryLineEntity>> getDeliveryLines(
            @PathVariable("id") String poId,
            @RequestParam String tenantId) {
        // Get first delivery for this PO and return its lines
        SupplierDeliveryEntity delivery = purchaseOrderService
                .getDelivery(tenantId,
                        purchaseOrderService.listPOs(tenantId).stream()
                                .filter(p -> p.getPoId().equals(poId))
                                .findFirst()
                                .map(p -> p.getPoId()).orElse(poId));
        return ResponseEntity.ok(purchaseOrderService.getDeliveryLines(delivery.getDeliveryId()));
    }

    // ── G-10: Reverse lookup + auto-PO from plan ─────────────────────────────

    /** GET /v2/purchase-orders/ingredients/{ingredientId}/suppliers?tenantId=... — reverse lookup */
    @GetMapping("/ingredients/{ingredientId}/suppliers")
    public ResponseEntity<List<SupplierCatalogItemEntity>> suppliersForIngredient(
            @PathVariable String ingredientId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(purchaseOrderService.findSuppliersForIngredient(tenantId, ingredientId));
    }

    /** POST /v2/purchase-orders/from-plan?tenantId=...&planId=... — auto-generate POs from plan */
    @PostMapping("/from-plan")
    public ResponseEntity<List<PurchaseOrderEntity>> generateFromPlan(
            @RequestParam String tenantId,
            @RequestParam String planId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.generatePOsFromPlan(tenantId, planId));
    }
}
