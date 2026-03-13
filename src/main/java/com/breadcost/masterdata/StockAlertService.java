package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import com.breadcost.domain.Recipe;
import com.breadcost.domain.WorkOrder;
import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * G-6: Low-stock detection and auto-production plan generation.
 * Scans items below minStockThreshold, identifies affected products,
 * and auto-creates DRAFT production plans for products with pending orders.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockAlertService {

    private final ItemRepository itemRepository;
    private final InventoryProjection inventoryProjection;
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductionPlanRepository planRepository;
    private final WorkOrderRepository workOrderRepository;
    private final InventoryService inventoryService;

    /**
     * Detect all items below their minimum stock threshold.
     * Returns structured alerts with severity and affected products.
     */
    public List<LowStockAlert> detectLowStock(String tenantId) {
        List<ItemEntity> items = itemRepository.findByTenantId(tenantId).stream()
                .filter(i -> i.isActive() && i.getMinStockThreshold() > 0)
                .toList();

        List<LowStockAlert> alerts = new ArrayList<>();
        for (ItemEntity item : items) {
            BigDecimal onHand = inventoryService.getTotalOnHand(tenantId, item.getItemId());
            double threshold = item.getMinStockThreshold();

            if (onHand.doubleValue() < threshold) {
                String severity = onHand.compareTo(BigDecimal.ZERO) <= 0 ? "CRITICAL" : "LOW";

                // Find recipes that use this ingredient
                List<String> affectedProductIds = findProductsUsingIngredient(tenantId, item.getItemId());

                alerts.add(new LowStockAlert(
                        item.getItemId(), item.getName(), item.getBaseUom(),
                        onHand, BigDecimal.valueOf(threshold), severity,
                        affectedProductIds
                ));
            }
        }
        return alerts;
    }

    /**
     * G-6: Auto-create a DRAFT production plan for products that have CONFIRMED orders
     * and can be produced (at least partially) with current stock.
     * Returns the created plan, or null if no actionable orders exist.
     */
    @Transactional
    public AutoPlanResult autoCreateProductionPlan(String tenantId, String siteId, String byUserId) {
        // Find CONFIRMED orders
        List<OrderEntity> confirmedOrders = orderRepository.findByTenantIdAndStatus(tenantId, "CONFIRMED");
        if (confirmedOrders.isEmpty()) {
            return new AutoPlanResult(null, List.of(), "No confirmed orders to plan");
        }

        // Aggregate demand by productId
        Map<String, Double> demandByProduct = new LinkedHashMap<>();
        for (OrderEntity order : confirmedOrders) {
            for (OrderLineEntity line : order.getLines()) {
                if (line.getProductId() != null) {
                    demandByProduct.merge(line.getProductId(), line.getQty(), Double::sum);
                }
            }
        }

        if (demandByProduct.isEmpty()) {
            return new AutoPlanResult(null, List.of(), "No product lines in confirmed orders");
        }

        // Create DRAFT plan for today
        ProductionPlanEntity plan = ProductionPlanEntity.builder()
                .planId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .siteId(siteId != null ? siteId : "MAIN")
                .planDate(LocalDate.now())
                .shift(ProductionPlan.Shift.MORNING)
                .status(ProductionPlan.Status.DRAFT)
                .notes("Auto-generated from stock alert service")
                .createdByUserId(byUserId != null ? byUserId : "system")
                .workOrders(new ArrayList<>())
                .build();

        List<String> warnings = new ArrayList<>();
        int woCount = 0;

        for (Map.Entry<String, Double> entry : demandByProduct.entrySet()) {
            String productId = entry.getKey();
            double totalQty = entry.getValue();

            // Find active recipe
            Optional<ProductEntity> productOpt = productRepository.findById(productId)
                    .filter(p -> tenantId.equals(p.getTenantId()));
            if (productOpt.isEmpty()) continue;

            ProductEntity product = productOpt.get();
            var recipes = recipeRepository.findByTenantIdAndProductIdAndStatus(
                    tenantId, productId, Recipe.RecipeStatus.ACTIVE);
            if (recipes.isEmpty()) {
                warnings.add("No active recipe for " + product.getName());
                continue;
            }

            RecipeEntity recipe = recipes.get(0);
            int batchCount = BigDecimal.valueOf(totalQty)
                    .divide(recipe.getBatchSize(), 0, RoundingMode.CEILING)
                    .intValue();
            if (batchCount < 1) batchCount = 1;

            // Check if materials are available
            var shortages = inventoryService.checkMaterialAvailability(
                    tenantId, recipe.getRecipeId(), batchCount);
            if (!shortages.isEmpty()) {
                String shortageDesc = shortages.stream()
                        .map(s -> s.itemName() + " (need " + s.required() + ", have " + s.onHand() + " " + s.uom() + ")")
                        .collect(Collectors.joining(", "));
                warnings.add(product.getName() + ": material shortage — " + shortageDesc);
            }

            WorkOrderEntity wo = WorkOrderEntity.builder()
                    .workOrderId(UUID.randomUUID().toString())
                    .plan(plan)
                    .tenantId(tenantId)
                    .departmentId(product.getDepartmentId())
                    .productId(productId)
                    .productName(product.getName())
                    .recipeId(recipe.getRecipeId())
                    .targetQty(totalQty)
                    .uom(recipe.getBatchSizeUom())
                    .batchCount(batchCount)
                    .status(WorkOrder.Status.PENDING)
                    .startOffsetHours(0)
                    .durationHours(recipe.getLeadTimeHours())
                    .build();

            plan.getWorkOrders().add(wo);
            woCount++;
        }

        if (woCount == 0) {
            return new AutoPlanResult(null, warnings, "No work orders could be generated");
        }

        plan.setStatus(ProductionPlan.Status.GENERATED);
        ProductionPlanEntity saved = planRepository.save(plan);

        log.info("Auto-created production plan {} with {} work orders for tenant {}",
                saved.getPlanId(), woCount, tenantId);

        return new AutoPlanResult(saved, warnings,
                "Plan created with " + woCount + " work orders");
    }

    private List<String> findProductsUsingIngredient(String tenantId, String itemId) {
        // Find active recipes where any ingredient uses this itemId
        return recipeRepository.findAll().stream()
                .filter(r -> tenantId.equals(r.getTenantId())
                        && r.getStatus() == Recipe.RecipeStatus.ACTIVE
                        && r.getIngredients() != null
                        && r.getIngredients().stream().anyMatch(i -> itemId.equals(i.getItemId())))
                .map(RecipeEntity::getProductId)
                .distinct()
                .toList();
    }

    public record LowStockAlert(
            String itemId, String itemName, String uom,
            BigDecimal onHand, BigDecimal threshold, String severity,
            List<String> affectedProductIds
    ) {}

    public record AutoPlanResult(
            ProductionPlanEntity plan,
            List<String> warnings,
            String message
    ) {}
}
