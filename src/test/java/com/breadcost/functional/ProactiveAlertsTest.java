package com.breadcost.functional;

import com.breadcost.domain.*;
import com.breadcost.masterdata.*;
import com.breadcost.mobile.MobileAppService;
import com.breadcost.mobile.PushNotificationRepository;
import com.breadcost.projections.InventoryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R5-S2 tests: Order status notifications (G-3), POS stock check (BC-5002),
 * catalog stock visibility (G-7), auto production plan (G-6).
 */
@DisplayName("R5-S2 :: Proactive Alerts — Notifications + Stock Visibility + Auto Plan")
class ProactiveAlertsTest extends FunctionalTestBase {

    @Autowired private ItemRepository itemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryProjection inventoryProjection;
    @Autowired private InventoryService inventoryService;
    @Autowired private OrderService orderService;
    @Autowired private StockAlertService stockAlertService;
    @Autowired private PushNotificationRepository pushNotificationRepository;

    private String flourId;
    private String sugarId;
    private String productId;
    private String recipeId;
    private String deptId;

    @BeforeEach
    void seed() {
        // Department
        deptId = "dept-r5s2-" + UUID.randomUUID().toString().substring(0, 6);
        departmentRepository.save(DepartmentEntity.builder()
                .departmentId(deptId)
                .tenantId(TENANT)
                .name("R5S2 Dept " + deptId)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        // Items
        flourId = ensureItem("flour-s2-" + deptId, "Flour S2", "INGREDIENT", "KG", 50.0);
        sugarId = ensureItem("sugar-s2-" + deptId, "Sugar S2", "INGREDIENT", "KG", 20.0);

        // Product
        productId = "prod-s2-" + deptId;
        productRepository.save(ProductEntity.builder()
                .productId(productId)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Test Bread S2 " + deptId)
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("8000"))
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        // Recipe: 5 KG flour + 1 KG sugar per batch of 10
        recipeId = "recipe-s2-" + deptId;
        recipeRepository.save(RecipeEntity.builder()
                .recipeId(recipeId)
                .tenantId(TENANT)
                .productId(productId)
                .versionNumber(1)
                .status(Recipe.RecipeStatus.ACTIVE)
                .batchSize(new BigDecimal("10"))
                .batchSizeUom("pcs")
                .expectedYield(new BigDecimal("10"))
                .yieldUom("pcs")
                .ingredients(List.of(
                        RecipeIngredientEntity.builder()
                                .ingredientLineId(UUID.randomUUID().toString())
                                .recipeId(recipeId)
                                .itemId(flourId)
                                .itemName("Flour S2")
                                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                                .recipeQty(new BigDecimal("5.0"))
                                .recipeUom("KG")
                                .purchasingUnitSize(new BigDecimal("1"))
                                .purchasingUom("KG")
                                .wasteFactor(new BigDecimal("0"))
                                .build(),
                        RecipeIngredientEntity.builder()
                                .ingredientLineId(UUID.randomUUID().toString())
                                .recipeId(recipeId)
                                .itemId(sugarId)
                                .itemName("Sugar S2")
                                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                                .recipeQty(new BigDecimal("1.0"))
                                .recipeUom("KG")
                                .purchasingUnitSize(new BigDecimal("1"))
                                .purchasingUom("KG")
                                .wasteFactor(new BigDecimal("0"))
                                .build()
                ))
                .build());

        // Seed 100 KG flour, 20 KG sugar
        seedStock(flourId, new BigDecimal("100"), "KG", new BigDecimal("2.00"));
        seedStock(sugarId, new BigDecimal("20"), "KG", new BigDecimal("5.00"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-3: Order status change triggers notification
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-3 ✓ advanceStatus triggers push notification to customer")
    void orderStatusChange_triggersNotification() {
        // Create and confirm an order
        OrderEntity order = orderService.createOrder(
                TENANT, "MAIN", "cust-r5s2", "Test Customer",
                "admin1", Instant.now().plusSeconds(86400), false, null,
                "test order",
                List.of(OrderService.CreateOrderLineRequest.builder()
                        .productId(productId).productName("Test Bread S2")
                        .departmentId(deptId).departmentName("R5S2 Dept")
                        .qty(5).uom("pcs").unitPrice(new BigDecimal("8000"))
                        .build()),
                UUID.randomUUID().toString());

        orderService.confirmOrder(TENANT, order.getOrderId(), "admin1");

        long notifsBefore = pushNotificationRepository.findByTenantIdAndCustomerId(
                TENANT, "cust-r5s2").size();

        // Advance to IN_PRODUCTION — should trigger notification
        orderService.advanceStatus(TENANT, order.getOrderId(),
                Order.Status.IN_PRODUCTION, "admin1");

        long notifsAfter = pushNotificationRepository.findByTenantIdAndCustomerId(
                TENANT, "cust-r5s2").size();

        assertTrue(notifsAfter > notifsBefore,
                "Push notification should be created on status change");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BC-5002: POS stock check
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BC-5002 ✓ POS stock-check returns empty when stock is sufficient")
    void posStockCheck_sufficient() throws Exception {
        var body = List.of(Map.of(
                "productId", productId,
                "productName", "Test Bread S2",
                "quantity", 1,
                "unit", "pcs",
                "unitPrice", 8000
        ));

        POST("/v1/pos/stock-check?tenantId=" + TENANT, body, bearer("cashier1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("BC-5002 ✓ POS stock-check returns warnings when stock insufficient")
    void posStockCheck_insufficient() throws Exception {
        // Request 10000 units → needs 5000 KG flour; only 100 KG available
        var body = List.of(Map.of(
                "productId", productId,
                "productName", "Test Bread S2",
                "quantity", 10000,
                "unit", "pcs",
                "unitPrice", 8000
        ));

        POST("/v1/pos/stock-check?tenantId=" + TENANT, body, bearer("cashier1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(productId))
                .andExpect(jsonPath("$[0].shortages", hasSize(greaterThan(0))));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-7: Catalog real-time stock
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-7 ✓ Catalog product includes inStock=true when materials available")
    void catalogProduct_inStock() throws Exception {
        GET("/v2/products/" + productId + "?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-6: Low stock detection + auto plan
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-6 ✓ detectLowStock returns alerts for items below threshold")
    void detectLowStock_returnsAlerts() {
        // Create an item with threshold higher than on-hand
        String lowItemId = ensureItem("low-item-" + deptId, "Low Item", "INGREDIENT", "KG", 9999.0);
        seedStock(lowItemId, new BigDecimal("1"), "KG", new BigDecimal("1.00"));

        var alerts = stockAlertService.detectLowStock(TENANT);

        assertTrue(alerts.stream().anyMatch(a -> a.itemId().equals(lowItemId)),
                "Should detect low stock for item with threshold 9999 and on-hand 1");
    }

    @Test
    @DisplayName("G-6 ✓ autoCreateProductionPlan generates plan from confirmed orders")
    void autoPlan_createsFromConfirmedOrders() {
        // Create and confirm an order for our product
        OrderEntity order = orderService.createOrder(
                TENANT, "MAIN", "cust-auto-plan", "Auto Plan Customer",
                "admin1", Instant.now().plusSeconds(86400), false, null,
                "auto plan test",
                List.of(OrderService.CreateOrderLineRequest.builder()
                        .productId(productId).productName("Test Bread S2")
                        .departmentId(deptId).departmentName("R5S2 Dept")
                        .qty(20).uom("pcs").unitPrice(new BigDecimal("8000"))
                        .build()),
                UUID.randomUUID().toString());

        orderService.confirmOrder(TENANT, order.getOrderId(), "admin1");

        var result = stockAlertService.autoCreateProductionPlan(TENANT, "MAIN", "admin1");

        assertNotNull(result.plan(), "Plan should be created");
        assertFalse(result.plan().getWorkOrders().isEmpty(), "Plan should have work orders");
        assertTrue(result.plan().getWorkOrders().stream()
                        .anyMatch(wo -> productId.equals(wo.getProductId())),
                "Plan should include WO for our product");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String ensureItem(String itemId, String name, String type, String uom, double threshold) {
        if (itemRepository.findById(itemId).isEmpty()) {
            ItemEntity item = new ItemEntity();
            item.setItemId(itemId);
            item.setTenantId(TENANT);
            item.setName(name);
            item.setType(type);
            item.setBaseUom(uom);
            item.setMinStockThreshold(threshold);
            item.setActive(true);
            itemRepository.save(item);
        }
        return itemId;
    }

    private void seedStock(String itemId, BigDecimal qty, String uom, BigDecimal unitCost) {
        var event = com.breadcost.events.ReceiveLotEvent.builder()
                .tenantId(TENANT)
                .siteId("MAIN")
                .itemId(itemId)
                .lotId("LOT-" + itemId + "-" + UUID.randomUUID().toString().substring(0, 4))
                .qty(qty)
                .uom(uom)
                .unitCostBase(unitCost)
                .occurredAtUtc(Instant.now())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        var eventStore = org.springframework.test.util.ReflectionTestUtils.getField(
                inventoryProjection, "eventStore");
        ((com.breadcost.eventstore.EventStore) eventStore)
                .appendEvent(event, com.breadcost.domain.LedgerEntry.EntryClass.FINANCIAL);
    }
}
