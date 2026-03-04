package com.breadcost.api;

import com.breadcost.domain.ProductionPlan;
import com.breadcost.masterdata.ProductionPlanEntity;
import com.breadcost.masterdata.ProductionPlanService;
import com.breadcost.masterdata.ProductionPlanService.PlanMaterialRequirement;
import com.breadcost.masterdata.ProductionPlanService.PlanSchedule;
import com.breadcost.masterdata.WorkOrderEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/production-plans")
@RequiredArgsConstructor
public class ProductionPlanController {

    private final ProductionPlanService planService;

    // ─── PLANS ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<List<ProductionPlanEntity>> getPlans(
            @RequestParam String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(planService.getPlans(tenantId, date, status));
    }

    @GetMapping("/{planId}")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<ProductionPlanEntity> getPlan(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return planService.getPlan(tenantId, planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ProductionPlanEntity> createPlan(@RequestBody CreatePlanRequest req) {
        ProductionPlanEntity created = planService.createPlan(
                req.getTenantId(), req.getSiteId(),
                req.getPlanDate(), req.getShift(),
                req.getNotes(), getPrincipalName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── PLAN ACTIONS ────────────────────────────────────────────────────────

    @PostMapping("/{planId}/generate")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ProductionPlanEntity> generateWorkOrders(
            @PathVariable String planId,
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "false") boolean forceRegenerate) {
        return ResponseEntity.ok(
                planService.generateWorkOrders(tenantId, planId, forceRegenerate, getPrincipalName())
        );
    }

    @PostMapping("/{planId}/publish")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ProductionPlanEntity> publishPlan(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.publishPlan(tenantId, planId));
    }

    @PostMapping("/{planId}/start")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ProductionPlanEntity> startPlan(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.startPlan(tenantId, planId));
    }

    @PostMapping("/{planId}/complete")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ProductionPlanEntity> completePlan(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.completePlan(tenantId, planId));
    }

    @GetMapping("/{planId}/material-requirements")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser')")
    public ResponseEntity<List<PlanMaterialRequirement>> getMaterialRequirements(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.getMaterialRequirements(tenantId, planId));
    }

    /** GET /{planId}/schedule?tenantId=xxx — returns work-order timeline + critical path */
    @GetMapping("/{planId}/schedule")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<PlanSchedule> getSchedule(
            @PathVariable String planId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.getSchedule(tenantId, planId));
    }

    /** PATCH /work-orders/{workOrderId}/schedule?tenantId=xxx — set offset + duration */
    @PatchMapping("/work-orders/{workOrderId}/schedule")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<WorkOrderEntity> patchSchedule(
            @PathVariable String workOrderId,
            @RequestParam String tenantId,
            @RequestBody SchedulePatchRequest req) {
        return ResponseEntity.ok(
                planService.patchWorkOrderSchedule(tenantId, workOrderId,
                        req.getStartOffsetHours(), req.getDurationHours()));
    }

    // ─── WORK ORDERS ─────────────────────────────────────────────────────────

    @GetMapping("/work-orders")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<List<WorkOrderEntity>> getWorkOrdersByDepartment(
            @RequestParam String tenantId,
            @RequestParam String departmentId) {
        return ResponseEntity.ok(planService.getWorkOrdersByDepartment(tenantId, departmentId));
    }

    @PostMapping("/work-orders/{workOrderId}/start")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<WorkOrderEntity> startWorkOrder(
            @PathVariable String workOrderId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.startWorkOrder(tenantId, workOrderId, getPrincipalName()));
    }

    @PostMapping("/work-orders/{workOrderId}/complete")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<WorkOrderEntity> completeWorkOrder(
            @PathVariable String workOrderId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.completeWorkOrder(tenantId, workOrderId));
    }

    @PostMapping("/work-orders/{workOrderId}/cancel")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<WorkOrderEntity> cancelWorkOrder(
            @PathVariable String workOrderId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(planService.cancelWorkOrder(tenantId, workOrderId));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String getPrincipalName() {
        return "system"; // TODO: replace with real Spring Security principal
    }

    // ─── REQUEST DTO ─────────────────────────────────────────────────────────

    @Data
    public static class CreatePlanRequest {
        private String tenantId;
        private String siteId;
        private LocalDate planDate;
        private ProductionPlan.Shift shift;
        private String notes;
    }

    @Data
    public static class SchedulePatchRequest {
        private Integer startOffsetHours;
        private Integer durationHours;
    }
}
