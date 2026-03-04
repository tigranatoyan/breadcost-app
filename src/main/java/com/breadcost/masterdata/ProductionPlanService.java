package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import com.breadcost.domain.WorkOrder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionPlanService {

    private final ProductionPlanRepository planRepository;
    private final WorkOrderRepository workOrderRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;

    // ─── CREATE EMPTY PLAN ────────────────────────────────────────────────────

    @Transactional
    public ProductionPlanEntity createPlan(String tenantId, String siteId,
                                           LocalDate planDate, ProductionPlan.Shift shift,
                                           String notes, String createdByUserId) {
        ProductionPlanEntity entity = ProductionPlanEntity.builder()
                .planId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .siteId(siteId)
                .planDate(planDate)
                .shift(shift)
                .status(ProductionPlan.Status.DRAFT)
                .notes(notes)
                .createdByUserId(createdByUserId)
                .workOrders(new ArrayList<>())
                .build();
        return planRepository.save(entity);
    }

    // ─── GENERATE WORK ORDERS FROM CONFIRMED ORDERS ───────────────────────────

    /**
     * Scan all CONFIRMED orders whose requestedDeliveryTime falls on planDate,
     * consolidate lines by (departmentId, productId), look up active recipe,
     * compute batchCount, and add WorkOrders to the plan.
     *
     * Idempotent: products already in the plan are skipped unless forceRegenerate=true.
     */
    @Transactional
    public ProductionPlanEntity generateWorkOrders(String tenantId, String planId,
                                                   boolean forceRegenerate, String byUserId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);
        if (plan.getStatus() != ProductionPlan.Status.DRAFT) {
            throw new IllegalStateException("Work orders can only be generated for DRAFT plans.");
        }

        // Find all CONFIRMED orders for this tenant
        List<OrderEntity> confirmedOrders = orderRepository.findByTenantIdAndStatus(tenantId, "CONFIRMED");

        // Prefer orders whose requestedDeliveryTime is on planDate; fall back to all confirmed orders
        LocalDate targetDate = plan.getPlanDate();
        List<OrderEntity> relevantOrders = confirmedOrders.stream()
                .filter(o -> o.getRequestedDeliveryTime() != null &&
                        o.getRequestedDeliveryTime()
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                                .equals(targetDate))
                .collect(Collectors.toList());

        // If no date-matched orders, use ALL confirmed orders so a plan can always be generated
        if (relevantOrders.isEmpty()) {
            relevantOrders = confirmedOrders;
        }

        // Collect all lines from relevant orders
        List<OrderLineEntity> allLines = relevantOrders.stream()
                .flatMap(o -> o.getLines().stream())
                .collect(Collectors.toList());

        if (allLines.isEmpty()) {
            return plan; // no confirmed orders at all
        }

        // Key: departmentId + "|" + productId → aggregated qty + line IDs
        Map<String, AggregatedLine> aggregated = new LinkedHashMap<>();
        for (OrderLineEntity line : allLines) {
            String key = line.getDepartmentId() + "|" + line.getProductId();
            aggregated.computeIfAbsent(key, k -> new AggregatedLine(
                    line.getDepartmentId(), line.getDepartmentName(),
                    line.getProductId(), line.getProductName(),
                    line.getUom()
            )).addLine(line.getOrderLineId(), line.getQty());
        }

        // If not forceRegenerate, skip products already in the plan
        if (!forceRegenerate) {
            Set<String> existing = plan.getWorkOrders().stream()
                    .map(wo -> wo.getDepartmentId() + "|" + wo.getProductId())
                    .collect(Collectors.toSet());
            aggregated.keySet().removeAll(existing);
        } else {
            // Remove existing work orders before regenerating
            plan.getWorkOrders().clear();
        }

        // Build work order entities
        for (AggregatedLine agg : aggregated.values()) {
            // Look up product → active recipe
            Optional<ProductEntity> productOpt = productRepository.findAll().stream()
                    .filter(p -> tenantId.equals(p.getTenantId()) && agg.productId.equals(p.getProductId()))
                    .findFirst();

            String recipeId = null;
            int batchCount = 1;
            Optional<RecipeEntity> recipeOpt = Optional.empty();

            if (productOpt.isPresent() && productOpt.get().getActiveRecipeId() != null) {
                recipeId = productOpt.get().getActiveRecipeId();
                recipeOpt = recipeRepository.findById(recipeId);
                if (recipeOpt.isPresent() && recipeOpt.get().getBatchSize() != null) {
                    BigDecimal batchSizeBD = recipeOpt.get().getBatchSize();
                    batchCount = new BigDecimal(agg.totalQty)
                            .divide(batchSizeBD, 0, RoundingMode.CEILING)
                            .intValue();
                    if (batchCount < 1) batchCount = 1;
                }
            }

            WorkOrderEntity wo = WorkOrderEntity.builder()
                    .workOrderId(UUID.randomUUID().toString())
                    .plan(plan)
                    .tenantId(tenantId)
                    .departmentId(agg.departmentId)
                    .departmentName(agg.departmentName)
                    .productId(agg.productId)
                    .productName(agg.productName)
                    .recipeId(recipeId)
                    .targetQty(agg.totalQty)
                    .uom(agg.uom)
                    .batchCount(batchCount)
                    .status(WorkOrder.Status.PENDING)
                    .sourceOrderLineIds(String.join(",", agg.lineIds))
                    .startOffsetHours(0)
                    .durationHours(recipeOpt.flatMap(r -> Optional.ofNullable(r.getLeadTimeHours())).orElse(null))
                    .build();

            plan.getWorkOrders().add(wo);
        }

        return planRepository.save(plan);
    }

    // ─── PLAN STATUS TRANSITIONS ─────────────────────────────────────────────

    @Transactional
    public ProductionPlanEntity publishPlan(String tenantId, String planId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);
        if (plan.getStatus() != ProductionPlan.Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT plans can be published.");
        }
        plan.setStatus(ProductionPlan.Status.PUBLISHED);
        return planRepository.save(plan);
    }

    @Transactional
    public ProductionPlanEntity startPlan(String tenantId, String planId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);
        if (plan.getStatus() != ProductionPlan.Status.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED plans can be started.");
        }
        plan.setStatus(ProductionPlan.Status.IN_PROGRESS);
        return planRepository.save(plan);
    }

    @Transactional
    public ProductionPlanEntity completePlan(String tenantId, String planId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);
        if (plan.getStatus() != ProductionPlan.Status.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS plans can be completed.");
        }
        plan.setStatus(ProductionPlan.Status.COMPLETED);
        return planRepository.save(plan);
    }

    // ─── WORK ORDER STATUS TRANSITIONS ───────────────────────────────────────

    @Transactional
    public WorkOrderEntity startWorkOrder(String tenantId, String workOrderId, String byUserId) {
        WorkOrderEntity wo = findWorkOrder(tenantId, workOrderId);
        if (wo.getStatus() != WorkOrder.Status.PENDING) {
            throw new IllegalStateException("Only PENDING work orders can be started.");
        }
        wo.setStatus(WorkOrder.Status.STARTED);
        wo.setStartedAt(Instant.now());
        wo.setAssignedToUserId(byUserId);
        return workOrderRepository.save(wo);
    }

    @Transactional
    public WorkOrderEntity completeWorkOrder(String tenantId, String workOrderId) {
        WorkOrderEntity wo = findWorkOrder(tenantId, workOrderId);
        if (wo.getStatus() != WorkOrder.Status.STARTED) {
            throw new IllegalStateException("Only STARTED work orders can be completed.");
        }
        wo.setStatus(WorkOrder.Status.COMPLETED);
        wo.setCompletedAt(Instant.now());
        WorkOrderEntity saved = workOrderRepository.save(wo);

        // Auto-advance plan to IN_PROGRESS when first WO starts; auto-complete when all WOs done
        ProductionPlanEntity plan = wo.getPlan();
        if (plan != null) {
            boolean allDone = plan.getWorkOrders().stream()
                    .allMatch(w -> w.getStatus() == WorkOrder.Status.COMPLETED
                            || w.getStatus() == WorkOrder.Status.CANCELLED);
            if (allDone && plan.getStatus() == ProductionPlan.Status.IN_PROGRESS) {
                plan.setStatus(ProductionPlan.Status.COMPLETED);
                planRepository.save(plan);
            }
        }
        return saved;
    }

    @Transactional
    public WorkOrderEntity cancelWorkOrder(String tenantId, String workOrderId) {
        WorkOrderEntity wo = findWorkOrder(tenantId, workOrderId);
        wo.setStatus(WorkOrder.Status.CANCELLED);
        return workOrderRepository.save(wo);
    }

    // ─── SCHEDULE MANAGEMENT ──────────────────────────────────────────────────

    /**
     * Update the scheduling parameters (start offset and/or duration) for a single work order.
     * startOffsetHours: hours after plan notional start when this WO begins (0 = simultaneous).
     * durationHours: expected production duration in hours (null = leave unchanged).
     */
    @Transactional
    public WorkOrderEntity patchWorkOrderSchedule(String tenantId, String workOrderId,
                                                   Integer startOffsetHours, Integer durationHours) {
        WorkOrderEntity wo = findWorkOrder(tenantId, workOrderId);
        if (startOffsetHours != null) {
            if (startOffsetHours < 0) throw new IllegalArgumentException("startOffsetHours must be >= 0");
            wo.setStartOffsetHours(startOffsetHours);
        }
        if (durationHours != null) {
            if (durationHours < 0) throw new IllegalArgumentException("durationHours must be >= 0");
            wo.setDurationHours(durationHours);
        }
        return workOrderRepository.save(wo);
    }

    /**
     * Return the full schedule for a plan: per-work-order offsets, durations,
     * overlap detection, critical-path total lead time.
     */
    public PlanSchedule getSchedule(String tenantId, String planId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);
        List<WorkOrderEntity> wos = plan.getWorkOrders().stream()
                .filter(w -> w.getStatus() != WorkOrder.Status.CANCELLED)
                .collect(Collectors.toList());

        int totalLeadTimeHours = wos.stream()
                .filter(w -> w.getStartOffsetHours() != null && w.getDurationHours() != null)
                .mapToInt(w -> w.getStartOffsetHours() + w.getDurationHours())
                .max().orElse(0);

        // Determine which work orders share time intervals (parallel)
        // A work order is on the critical path if its end == totalLeadTimeHours
        List<PlanScheduleEntry> entries = wos.stream().map(wo -> {
            int start = wo.getStartOffsetHours() != null ? wo.getStartOffsetHours() : 0;
            Integer dur = wo.getDurationHours();
            Integer end = dur != null ? start + dur : null;
            boolean onCriticalPath = end != null && end == totalLeadTimeHours;

            // Parallel = overlaps with at least one other WO (intervening time ranges intersect)
            boolean parallel = wos.stream().filter(other -> !other.getWorkOrderId().equals(wo.getWorkOrderId()))
                    .anyMatch(other -> {
                        int os = other.getStartOffsetHours() != null ? other.getStartOffsetHours() : 0;
                        Integer od = other.getDurationHours();
                        if (od == null) return false;
                        int oe = os + od;
                        // overlap: start < oe && end > os
                        return (dur != null) && start < oe && end > os;
                    });

            return new PlanScheduleEntry(
                    wo.getWorkOrderId(), wo.getProductName(), wo.getDepartmentName(),
                    wo.getStatus().name(), start, dur, end, parallel, onCriticalPath,
                    wo.getBatchCount(), wo.getTargetQty(), wo.getUom());
        }).collect(Collectors.toList());

        return new PlanSchedule(plan.getPlanId(), plan.getPlanDate().toString(),
                plan.getShift().name(), plan.getStatus().name(),
                totalLeadTimeHours, entries);
    }

    public record PlanScheduleEntry(
            String workOrderId, String productName, String departmentName,
            String status, int startOffsetHours, Integer durationHours, Integer endOffsetHours,
            boolean parallel, boolean criticalPath, int batchCount, double targetQty, String uom) {}

    public record PlanSchedule(
            String planId, String planDate, String shift, String status,
            int totalLeadTimeHours, List<PlanScheduleEntry> workOrders) {}

    // ─── MATERIAL REQUIREMENTS ────────────────────────────────────────────────

    /**
     * Aggregate material requirements across all work orders in a plan.
     * Returns a shopping list: itemId → totalPurchasingUnits needed.
     */
    public List<PlanMaterialRequirement> getMaterialRequirements(String tenantId, String planId) {
        ProductionPlanEntity plan = findPlan(tenantId, planId);

        Map<String, PlanMaterialRequirement> consolidated = new LinkedHashMap<>();

        for (WorkOrderEntity wo : plan.getWorkOrders()) {
            if (wo.getRecipeId() == null || wo.getStatus() == WorkOrder.Status.CANCELLED) continue;

            Optional<RecipeEntity> recipeOpt = recipeRepository.findById(wo.getRecipeId());
            if (recipeOpt.isEmpty() || recipeOpt.get().getIngredients() == null) continue;

            int multiplier = wo.getBatchCount();

            for (RecipeIngredientEntity ing : recipeOpt.get().getIngredients()) {
                BigDecimal totalWeight = switch (ing.getUnitMode()) {
                    case WEIGHT -> ing.getRecipeQty() != null ? ing.getRecipeQty() : BigDecimal.ZERO;
                    case PIECE, COMBO -> (ing.getWeightPerPiece() != null && ing.getPieceQty() != null)
                            ? ing.getWeightPerPiece().multiply(BigDecimal.valueOf(ing.getPieceQty()))
                            : BigDecimal.ZERO;
                };

                BigDecimal wasteFactor = ing.getWasteFactor() != null ? ing.getWasteFactor() : BigDecimal.ZERO;
                BigDecimal withWaste = totalWeight
                        .multiply(BigDecimal.ONE.add(wasteFactor))
                        .multiply(BigDecimal.valueOf(multiplier));

                BigDecimal pus = ing.getPurchasingUnitSize();
                double purchasingUnits = (pus != null && pus.compareTo(BigDecimal.ZERO) > 0)
                        ? withWaste.divide(pus, 4, RoundingMode.CEILING).doubleValue()
                        : withWaste.doubleValue();

                consolidated.merge(
                        ing.getItemId(),
                        new PlanMaterialRequirement(ing.getItemId(), ing.getItemName(),
                                ing.getPurchasingUom(), purchasingUnits, wo.getWorkOrderId()),
                        (existing, newVal) -> new PlanMaterialRequirement(
                                existing.itemId(), existing.itemName(),
                                existing.purchasingUom(),
                                round4(existing.purchasingUnitsNeeded() + newVal.purchasingUnitsNeeded()),
                                null)
                );
            }
        }
        return new ArrayList<>(consolidated.values());
    }

    private double round4(double v) {
        return new BigDecimal(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    // ─── QUERIES ─────────────────────────────────────────────────────────────

    public List<ProductionPlanEntity> getPlans(String tenantId, LocalDate date, String status) {
        if (date != null) {
            return planRepository.findByTenantIdAndPlanDate(tenantId, date);
        }
        if (status != null) {
            return planRepository.findByTenantIdAndStatus(tenantId, ProductionPlan.Status.valueOf(status.toUpperCase()));
        }
        return planRepository.findByTenantId(tenantId);
    }

    public Optional<ProductionPlanEntity> getPlan(String tenantId, String planId) {
        return planRepository.findById(planId)
                .filter(p -> tenantId.equals(p.getTenantId()));
    }

    public List<WorkOrderEntity> getWorkOrdersByDepartment(String tenantId, String departmentId) {
        return workOrderRepository.findByTenantIdAndDepartmentId(tenantId, departmentId);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private ProductionPlanEntity findPlan(String tenantId, String planId) {
        return planRepository.findById(planId)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
    }

    private WorkOrderEntity findWorkOrder(String tenantId, String workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .filter(wo -> tenantId.equals(wo.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("WorkOrder not found: " + workOrderId));
    }

    // ─── INNER HELPERS ───────────────────────────────────────────────────────

    private static class AggregatedLine {
        final String departmentId, departmentName, productId, productName, uom;
        double totalQty = 0;
        final List<String> lineIds = new ArrayList<>();

        AggregatedLine(String deptId, String deptName, String prodId, String prodName, String uom) {
            this.departmentId = deptId;
            this.departmentName = deptName;
            this.productId = prodId;
            this.productName = prodName;
            this.uom = uom;
        }

        void addLine(String lineId, double qty) {
            this.lineIds.add(lineId);
            this.totalQty += qty;
        }
    }

    public record PlanMaterialRequirement(
            String itemId,
            String itemName,
            String purchasingUom,
            double purchasingUnitsNeeded,
            String contributingWorkOrderId
    ) {}
}
