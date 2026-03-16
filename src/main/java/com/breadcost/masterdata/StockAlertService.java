package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import com.breadcost.domain.Recipe;
import com.breadcost.domain.WorkOrder;
import com.breadcost.purchaseorder.PurchaseOrderEntity;
import com.breadcost.purchaseorder.PurchaseOrderService;
import com.breadcost.supplier.SupplierCatalogItemEntity;
import com.breadcost.supplier.SupplierCatalogItemRepository;
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
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductionPlanRepository planRepository;
    private final InventoryService inventoryService;
    private final PurchaseOrderService purchaseOrderService;
    private final SupplierCatalogItemRepository catalogItemRepo;

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

        Map<String, Double> demandByProduct = aggregateDemandByProduct(confirmedOrders);
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

        for (Map.Entry<String, Double> entry : demandByProduct.entrySet()) {
            buildWorkOrder(tenantId, plan, entry.getKey(), entry.getValue(), warnings)
                    .ifPresent(wo -> plan.getWorkOrders().add(wo));
        }

        if (plan.getWorkOrders().isEmpty()) {
            return new AutoPlanResult(null, warnings, "No work orders could be generated");
        }

        plan.setStatus(ProductionPlan.Status.GENERATED);
        plan.setNotes("Auto-generated from confirmed orders");
        ProductionPlanEntity saved = planRepository.save(plan);

        log.info("Auto-created production plan {} with {} work orders for tenant {}",
                saved.getPlanId(), plan.getWorkOrders().size(), tenantId);

        return new AutoPlanResult(saved, warnings,
                "Plan created with " + plan.getWorkOrders().size() + " work orders");
    }

    private Map<String, Double> aggregateDemandByProduct(List<OrderEntity> orders) {
        Map<String, Double> demandByProduct = new LinkedHashMap<>();
        for (OrderEntity order : orders) {
            for (OrderLineEntity line : order.getLines()) {
                if (line.getProductId() != null) {
                    demandByProduct.merge(line.getProductId(), line.getQty(), Double::sum);
                }
            }
        }
        return demandByProduct;
    }

    private Optional<WorkOrderEntity> buildWorkOrder(String tenantId, ProductionPlanEntity plan,
                                                      String productId, double totalQty,
                                                      List<String> warnings) {
        Optional<ProductEntity> productOpt = productRepository.findById(productId)
                .filter(p -> tenantId.equals(p.getTenantId()));
        if (productOpt.isEmpty()) {
            return Optional.empty();
        }

        ProductEntity product = productOpt.get();
        var recipes = recipeRepository.findByTenantIdAndProductIdAndStatus(
                tenantId, productId, Recipe.RecipeStatus.ACTIVE);
        if (recipes.isEmpty()) {
            warnings.add("No active recipe for " + product.getName());
            return Optional.empty();
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

        return Optional.of(WorkOrderEntity.builder()
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
                .build());
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

    /**
     * R5: Auto-generate purchase orders for low-stock ingredients.
     * Finds ingredients below threshold, matches to preferred suppliers,
     * and creates POs grouped by supplier.
     */
    @Transactional
    public AutoPurchaseOrderResult autoGeneratePurchaseOrders(String tenantId) {
        List<LowStockAlert> alerts = detectLowStock(tenantId);
        if (alerts.isEmpty()) {
            return new AutoPurchaseOrderResult(List.of(), List.of(), "No low-stock items detected");
        }

        List<String> warnings = new ArrayList<>();
        // Group order lines by supplier
        Map<String, List<PurchaseOrderService.LineInput>> bySupplierId = new LinkedHashMap<>();

        for (LowStockAlert alert : alerts) {
            List<SupplierCatalogItemEntity> suppliers =
                    catalogItemRepo.findByTenantIdAndIngredientId(tenantId, alert.itemId());
            if (suppliers.isEmpty()) {
                warnings.add("No supplier for " + alert.itemName());
                continue;
            }
            // Pick preferred or cheapest
            SupplierCatalogItemEntity best = suppliers.stream()
                    .sorted(Comparator.<SupplierCatalogItemEntity, Boolean>comparing(s -> !s.isPreferred())
                            .thenComparing(SupplierCatalogItemEntity::getUnitPrice))
                    .findFirst().orElse(suppliers.get(0));

            BigDecimal orderQty = alert.threshold().subtract(alert.onHand()).max(BigDecimal.ONE);

            bySupplierId.computeIfAbsent(best.getSupplierId(), k -> new ArrayList<>())
                    .add(new PurchaseOrderService.LineInput(
                            alert.itemId(), alert.itemName(),
                            orderQty.doubleValue(), alert.uom(),
                            best.getUnitPrice(), best.getCurrency()));
        }

        if (bySupplierId.isEmpty()) {
            return new AutoPurchaseOrderResult(List.of(), warnings, "No suppliers available for low-stock items");
        }

        List<PurchaseOrderEntity> createdPOs = new ArrayList<>();
        for (var entry : bySupplierId.entrySet()) {
            PurchaseOrderEntity po = purchaseOrderService.createPO(
                    tenantId, entry.getKey(), entry.getValue(),
                    "Auto-generated from stock alerts", 1.0, "USD");
            createdPOs.add(po);
        }

        log.info("Auto-generated {} POs for {} low-stock ingredients (tenant={})",
                createdPOs.size(), alerts.size(), tenantId);

        return new AutoPurchaseOrderResult(createdPOs, warnings,
                "Generated " + createdPOs.size() + " purchase orders");
    }

    public record AutoPurchaseOrderResult(
            List<PurchaseOrderEntity> purchaseOrders,
            List<String> warnings,
            String message
    ) {}
}
