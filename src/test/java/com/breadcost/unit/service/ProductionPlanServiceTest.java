package com.breadcost.unit.service;

import com.breadcost.domain.ProductionPlan;
import com.breadcost.domain.WorkOrder;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

    @Mock private ProductionPlanRepository planRepo;
    @Mock private WorkOrderRepository woRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private ProductRepository productRepo;
    @Mock private RecipeRepository recipeRepo;
    @Mock private InventoryService inventoryService;
    @InjectMocks private ProductionPlanService svc;

    // ── createPlan ───────────────────────────────────────────────────────────

    @Test
    void createPlan_draftStatus() {
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var plan = svc.createPlan("t1", "s1", LocalDate.of(2026, 3, 15),
                ProductionPlan.Shift.MORNING, "Test plan", "user1");

        assertEquals(ProductionPlan.Status.DRAFT, plan.getStatus());
        assertEquals("t1", plan.getTenantId());
        assertEquals(ProductionPlan.Shift.MORNING, plan.getShift());
    }

    // ── approvePlan ──────────────────────────────────────────────────────────

    @Test
    void approvePlan_generatedPlan_setsApproved() {
        var plan = plan("t1", "p1", ProductionPlan.Status.GENERATED);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.approvePlan("t1", "p1");

        assertEquals(ProductionPlan.Status.APPROVED, result.getStatus());
    }

    @Test
    void approvePlan_draftPlan_throws() {
        var plan = plan("t1", "p1", ProductionPlan.Status.DRAFT);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));

        assertThrows(IllegalStateException.class, () -> svc.approvePlan("t1", "p1"));
    }

    @Test
    void approvePlan_wrongTenant_throws() {
        var plan = plan("t2", "p1", ProductionPlan.Status.GENERATED);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));

        assertThrows(NoSuchElementException.class, () -> svc.approvePlan("t1", "p1"));
    }

    // ── startPlan ────────────────────────────────────────────────────────────

    @Test
    void startPlan_approvedPlan_setsInProgress() {
        var plan = plan("t1", "p1", ProductionPlan.Status.APPROVED);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.startPlan("t1", "p1");

        assertEquals(ProductionPlan.Status.IN_PROGRESS, result.getStatus());
    }

    @Test
    void startPlan_draftPlan_throws() {
        var plan = plan("t1", "p1", ProductionPlan.Status.DRAFT);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));

        assertThrows(IllegalStateException.class, () -> svc.startPlan("t1", "p1"));
    }

    // ── completePlan ─────────────────────────────────────────────────────────

    @Test
    void completePlan_inProgressPlan_setsCompleted() {
        var plan = plan("t1", "p1", ProductionPlan.Status.IN_PROGRESS);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.completePlan("t1", "p1");

        assertEquals(ProductionPlan.Status.COMPLETED, result.getStatus());
    }

    @Test
    void completePlan_approvedPlan_throws() {
        var plan = plan("t1", "p1", ProductionPlan.Status.APPROVED);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));

        assertThrows(IllegalStateException.class, () -> svc.completePlan("t1", "p1"));
    }

    // ── startWorkOrder ───────────────────────────────────────────────────────

    @Test
    void startWO_pending_setsStarted() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.startWorkOrder("t1", "wo1", "user1");

        assertEquals(WorkOrder.Status.STARTED, result.getStatus());
        assertEquals("user1", result.getAssignedToUserId());
        assertNotNull(result.getStartedAt());
    }

    @Test
    void startWO_notPending_throws() {
        var wo = wo("t1", "wo1", WorkOrder.Status.STARTED, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));

        assertThrows(IllegalStateException.class, () -> svc.startWorkOrder("t1", "wo1", "user1"));
    }

    @Test
    void startWO_materialShortage_throws() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, "recipe-1");
        wo.setBatchCount(2);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(inventoryService.checkMaterialAvailability("t1", "recipe-1", 2))
                .thenReturn(List.of(new InventoryService.MaterialShortage(
                        "flour", "Flour", new java.math.BigDecimal("10"),
                        new java.math.BigDecimal("3"), new java.math.BigDecimal("7"), "KG")));

        var ex = assertThrows(IllegalStateException.class,
                () -> svc.startWorkOrder("t1", "wo1", "user1"));
        assertTrue(ex.getMessage().contains("Insufficient materials"));
    }

    @Test
    void startWO_noShortages_succeeds() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, "recipe-1");
        wo.setBatchCount(1);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(inventoryService.checkMaterialAvailability("t1", "recipe-1", 1)).thenReturn(List.of());
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.startWorkOrder("t1", "wo1", "user1");

        assertEquals(WorkOrder.Status.STARTED, result.getStatus());
    }

    // ── completeWorkOrder ────────────────────────────────────────────────────

    @Test
    void completeWO_started_setsCompleted() {
        var wo = wo("t1", "wo1", WorkOrder.Status.STARTED, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.completeWorkOrder("t1", "wo1");

        assertEquals(WorkOrder.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void completeWO_notStarted_throws() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));

        assertThrows(IllegalStateException.class, () -> svc.completeWorkOrder("t1", "wo1"));
    }

    @Test
    void completeWO_withYield_recordsVariance() {
        var wo = wo("t1", "wo1", WorkOrder.Status.STARTED, "recipe-1");
        wo.setBatchCount(2);
        var recipe = RecipeEntity.builder().recipeId("recipe-1")
                .expectedYield(new java.math.BigDecimal("50")).build();
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(recipeRepo.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.completeWorkOrder("t1", "wo1", 90.0, 5.0, "A", "Good quality");

        assertEquals(90.0, result.getActualYield());
        assertEquals(5.0, result.getWasteQty());
        assertEquals("A", result.getQualityScore());
        // Expected: 50 * 2 = 100, actual: 90, variance: (90-100)/100 * 100 = -10%
        assertNotNull(result.getYieldVariancePct());
        assertEquals(-10.0, result.getYieldVariancePct(), 0.01);
    }

    @Test
    void completeWO_allDone_autoCompletesPlan() {
        var plan = plan("t1", "plan1", ProductionPlan.Status.IN_PROGRESS);
        var wo1 = wo("t1", "wo1", WorkOrder.Status.STARTED, null);
        var wo2 = wo("t1", "wo2", WorkOrder.Status.COMPLETED, null);
        wo1.setPlan(plan);
        wo2.setPlan(plan);
        plan.setWorkOrders(new ArrayList<>(List.of(wo1, wo2)));

        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo1));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.completeWorkOrder("t1", "wo1");

        assertEquals(ProductionPlan.Status.COMPLETED, plan.getStatus());
    }

    // ── cancelWorkOrder ──────────────────────────────────────────────────────

    @Test
    void cancelWO_setsStatusCancelled() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.cancelWorkOrder("t1", "wo1");

        assertEquals(WorkOrder.Status.CANCELLED, result.getStatus());
    }

    // ── patchWorkOrderSchedule ───────────────────────────────────────────────

    @Test
    void patchSchedule_updatesOffsetAndDuration() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));
        when(woRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.patchWorkOrderSchedule("t1", "wo1", 2, 4);

        assertEquals(2, result.getStartOffsetHours());
        assertEquals(4, result.getDurationHours());
    }

    @Test
    void patchSchedule_negativeOffset_throws() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));

        assertThrows(IllegalArgumentException.class,
                () -> svc.patchWorkOrderSchedule("t1", "wo1", -1, null));
    }

    @Test
    void patchSchedule_negativeDuration_throws() {
        var wo = wo("t1", "wo1", WorkOrder.Status.PENDING, null);
        when(woRepo.findById("wo1")).thenReturn(Optional.of(wo));

        assertThrows(IllegalArgumentException.class,
                () -> svc.patchWorkOrderSchedule("t1", "wo1", null, -1));
    }

    // ── generateWorkOrders ───────────────────────────────────────────────────

    @Test
    void generateWOs_nonDraftPlan_throws() {
        var plan = plan("t1", "p1", ProductionPlan.Status.APPROVED);
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));

        assertThrows(IllegalStateException.class,
                () -> svc.generateWorkOrders("t1", "p1", false, "user1"));
    }

    @Test
    void generateWOs_noOrders_setsGenerated() {
        var plan = plan("t1", "p1", ProductionPlan.Status.DRAFT);
        plan.setPlanDate(LocalDate.of(2026, 3, 20));
        plan.setWorkOrders(new ArrayList<>());
        when(planRepo.findById("p1")).thenReturn(Optional.of(plan));
        when(orderRepo.findByTenantIdAndStatus("t1", "CONFIRMED")).thenReturn(List.of());
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.generateWorkOrders("t1", "p1", false, "user1");

        assertEquals(ProductionPlan.Status.GENERATED, result.getStatus());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ProductionPlanEntity plan(String tenantId, String planId, ProductionPlan.Status status) {
        return ProductionPlanEntity.builder()
                .planId(planId).tenantId(tenantId).status(status)
                .shift(ProductionPlan.Shift.MORNING)
                .planDate(LocalDate.of(2026, 3, 15))
                .workOrders(new ArrayList<>())
                .build();
    }

    private WorkOrderEntity wo(String tenantId, String woId, WorkOrder.Status status, String recipeId) {
        return WorkOrderEntity.builder()
                .workOrderId(woId).tenantId(tenantId).status(status)
                .recipeId(recipeId).batchCount(1).targetQty(10.0)
                .build();
    }
}
