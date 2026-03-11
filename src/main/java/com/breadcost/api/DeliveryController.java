package com.breadcost.api;

import com.breadcost.delivery.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Delivery run REST API — BC-E14
 *
 * POST /v2/delivery-runs                               — BC-1401: create run
 * POST /v2/delivery-runs/{id}/orders                   — BC-1401: assign order
 * GET  /v2/delivery-runs/{id}/orders                   — list assigned orders
 * GET  /v2/delivery-runs/{id}/manifest                 — BC-1402: manifest
 * PUT  /v2/delivery-runs/{id}/orders/{orderId}/complete — BC-1403
 * PUT  /v2/delivery-runs/{id}/orders/{orderId}/fail     — BC-1404
 * POST /v2/delivery-runs/{id}/orders/{orderId}/redeliver — BC-1404: re-assign
 * GET  /v2/delivery-runs?tenantId=...                  — list runs
 * PUT  /v2/delivery-runs/{id}/orders/{orderId}/waive   — BC-1406: waive charge
 */
@RestController
@RequestMapping("/v2/delivery-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager')")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data
    public static class CreateRunRequest {
        @NotBlank private String tenantId;
        private String driverId;
        private String driverName;
        private LocalDate scheduledDate;
        private BigDecimal courierCharge;
        private String notes;
    }

    @Data
    public static class AssignOrderRequest {
        @NotBlank private String tenantId;
        @NotBlank private String orderId;
    }

    @Data
    public static class FailOrderRequest {
        @NotBlank private String tenantId;
        private String failureReason;
    }

    @Data
    public static class ReDeliverRequest {
        @NotBlank private String tenantId;
        @NotBlank private String newRunId;
    }

    @Data
    public static class WaiveChargeRequest {
        @NotBlank private String tenantId;
        @NotBlank private String waivedBy;
    }

    // ── BC-1401: Create run ───────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<DeliveryRunEntity> createRun(
            @Valid @RequestBody CreateRunRequest req) {
        DeliveryRunEntity run = deliveryService.createRun(
                req.getTenantId(), req.getDriverId(), req.getDriverName(),
                req.getScheduledDate(), req.getCourierCharge(), req.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(run);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryRunEntity>> listRuns(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(deliveryService.listRuns(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryRunEntity> getRun(
            @PathVariable("id") String runId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(deliveryService.getRun(tenantId, runId));
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<DeliveryRunOrderEntity> assignOrder(
            @PathVariable("id") String runId,
            @Valid @RequestBody AssignOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryService.assignOrder(req.getTenantId(), runId, req.getOrderId()));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<DeliveryRunOrderEntity>> getRunOrders(
            @PathVariable("id") String runId) {
        return ResponseEntity.ok(deliveryService.getRunOrders(runId));
    }

    // ── BC-1402: Manifest ─────────────────────────────────────────────────────

    @GetMapping("/{id}/manifest")
    public ResponseEntity<DeliveryService.ManifestResult> getManifest(
            @PathVariable("id") String runId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(deliveryService.getManifest(tenantId, runId));
    }

    // ── BC-1403: Complete ─────────────────────────────────────────────────────

    @PutMapping("/{id}/orders/{orderId}/complete")
    public ResponseEntity<DeliveryRunOrderEntity> completeOrder(
            @PathVariable("id") String runId,
            @PathVariable("orderId") String orderId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(deliveryService.markOrderCompleted(tenantId, runId, orderId));
    }

    // ── BC-1404: Failed + re-delivery ─────────────────────────────────────────

    @PutMapping("/{id}/orders/{orderId}/fail")
    public ResponseEntity<DeliveryRunOrderEntity> failOrder(
            @PathVariable("id") String runId,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody FailOrderRequest req) {
        return ResponseEntity.ok(
                deliveryService.markOrderFailed(req.getTenantId(), runId, orderId, req.getFailureReason()));
    }

    @PostMapping("/{id}/orders/{orderId}/redeliver")
    public ResponseEntity<DeliveryRunOrderEntity> reDeliver(
            @PathVariable("id") String runId,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody ReDeliverRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                deliveryService.reAssignOrder(req.getTenantId(), runId, orderId, req.getNewRunId()));
    }

    // ── BC-1406: Waiver ───────────────────────────────────────────────────────

    @PutMapping("/{id}/orders/{orderId}/waive")
    public ResponseEntity<DeliveryRunOrderEntity> waiveCourierCharge(
            @PathVariable("id") String runId,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody WaiveChargeRequest req) {
        return ResponseEntity.ok(
                deliveryService.waiveCourierCharge(req.getTenantId(), runId, orderId, req.getWaivedBy()));
    }
}
