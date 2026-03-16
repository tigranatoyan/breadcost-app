package com.breadcost.functional;

import com.breadcost.domain.Department;
import com.breadcost.domain.Product;
import com.breadcost.domain.Recipe;
import com.breadcost.domain.RecipeIngredient;
import com.breadcost.masterdata.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R5-S1 tests: POS inventory deduction (G-1), WO material check (G-2),
 * WO completion backflush.
 */
@DisplayName("R5-S1 :: Operational Integrity — Stock Deduction + Material Checks")
class OperationalIntegrityTest extends FunctionalTestBase {

    @Autowired private ItemRepository itemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private InventoryProjection inventoryProjection;
    @Autowired private InventoryService inventoryService;

    private String flourId;
    private String sugarId;
    private String breadProductId;
    private String breadRecipeId;

    @BeforeEach
    void seedProductAndRecipe() {
        // Seed items if not present
        flourId = ensureItem("flour-test-r5", "Flour R5", "INGREDIENT", "KG");
        sugarId = ensureItem("sugar-test-r5", "Sugar R5", "INGREDIENT", "KG");
        breadProductId = ensureProduct("bread-r5", "White Bread R5");
        breadRecipeId = ensureRecipeWithIngredients();

        // Seed inventory: 100 KG flour, 20 KG sugar
        seedStock(flourId, new BigDecimal("100"), "KG", new BigDecimal("2.00"));
        seedStock(sugarId, new BigDecimal("20"), "KG", new BigDecimal("5.00"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-1: POS sale deducts inventory
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-1 ✓ POS sale triggers inventory deduction via BackflushConsumption")
    void posSale_deductsInventory() throws Exception {
        BigDecimal flourBefore = inventoryService.getTotalOnHand(TENANT, flourId);
        BigDecimal sugarBefore = inventoryService.getTotalOnHand(TENANT, sugarId);

        var body = Map.of(
                "tenantId", TENANT,
                "siteId", "MAIN",
                "paymentMethod", "CASH",
                "cashReceived", 20000,
                "lines", List.of(
                        Map.of("productId", breadProductId, "productName", "White Bread R5",
                                "quantity", 1, "unit", "pcs", "unitPrice", 8000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        BigDecimal flourAfter = inventoryService.getTotalOnHand(TENANT, flourId);
        BigDecimal sugarAfter = inventoryService.getTotalOnHand(TENANT, sugarId);

        // Flour should have decreased (recipe calls for 5 KG per batch)
        assertTrue(flourAfter.compareTo(flourBefore) < 0,
                "Flour should decrease after POS sale — before=" + flourBefore + " after=" + flourAfter);
        // Sugar should have decreased (recipe calls for 1 KG per batch)
        assertTrue(sugarAfter.compareTo(sugarBefore) < 0,
                "Sugar should decrease after POS sale — before=" + sugarBefore + " after=" + sugarAfter);
    }

    @Test
    @DisplayName("G-1 ✓ POS sale without active recipe still succeeds (no deduction)")
    void posSale_noRecipe_stillSucceeds() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "siteId", "MAIN",
                "paymentMethod", "CASH",
                "cashReceived", 10000,
                "lines", List.of(
                        Map.of("productId", "no-recipe-product", "productName", "Mystery Item",
                                "quantity", 1, "unit", "pcs", "unitPrice", 5000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // G-2: WO material check on start
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("G-2 ✓ checkMaterialAvailability returns empty when stock is sufficient")
    void materialCheck_sufficientStock_noShortages() {
        var shortages = inventoryService.checkMaterialAvailability(
                TENANT, breadRecipeId, 1);
        assertTrue(shortages.isEmpty(), "No shortages expected when stock is sufficient");
    }

    @Test
    @DisplayName("G-2 ✓ checkMaterialAvailability detects shortage when stock insufficient")
    void materialCheck_insufficientStock_returnsShortages() {
        // Request 100 batches → 500 KG flour needed, only 100 available
        var shortages = inventoryService.checkMaterialAvailability(
                TENANT, breadRecipeId, 100);
        assertFalse(shortages.isEmpty(), "Should detect flour shortage for 100 batches");
        assertTrue(shortages.stream().anyMatch(s -> s.itemId().equals(flourId)),
                "Flour should be in shortage list");
    }

    @Test
    @DisplayName("G-2 ✓ consumeIngredients emits events and reduces stock")
    void consumeIngredients_reducesStock() {
        BigDecimal before = inventoryService.getTotalOnHand(TENANT, flourId);

        inventoryService.consumeIngredients(
                TENANT, "MAIN", breadRecipeId, 1, "TEST", "test-ref-1");

        BigDecimal after = inventoryService.getTotalOnHand(TENANT, flourId);
        assertTrue(after.compareTo(before) < 0,
                "Flour should decrease after consuming ingredients");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String ensureItem(String itemId, String name, String type, String uom) {
        if (itemRepository.findById(itemId).isEmpty()) {
            ItemEntity item = new ItemEntity();
            item.setItemId(itemId);
            item.setTenantId(TENANT);
            item.setName(name);
            item.setType(type);
            item.setBaseUom(uom);
            item.setActive(true);
            itemRepository.save(item);
        }
        return itemId;
    }

    private String ensureDepartment(String deptId, String deptName) {
        if (departmentRepository.findById(deptId).isEmpty()) {
            departmentRepository.save(DepartmentEntity.builder()
                    .departmentId(deptId)
                    .tenantId(TENANT)
                    .name(deptName)
                    .warehouseMode(Department.WarehouseMode.SHARED)
                    .status(Department.DepartmentStatus.ACTIVE)
                    .createdAtUtc(Instant.now())
                    .updatedAtUtc(Instant.now())
                    .build());
        }
        return deptId;
    }

    private String ensureProduct(String productId, String name) {
        if (productRepository.findById(productId).isEmpty()) {
            String deptId = ensureDepartment("dept-r5", "R5 Test Dept");
            productRepository.save(ProductEntity.builder()
                    .productId(productId)
                    .tenantId(TENANT)
                    .departmentId(deptId)
                    .name(name)
                    .saleUnit(Product.SaleUnit.PIECE)
                    .baseUom("PC")
                    .price(new BigDecimal("8000"))
                    .status(Product.ProductStatus.ACTIVE)
                    .createdAtUtc(Instant.now())
                    .updatedAtUtc(Instant.now())
                    .build());
        }
        return productId;
    }

    private String ensureRecipeWithIngredients() {
        String recipeId = "recipe-bread-r5";
        if (recipeRepository.findById(recipeId).isPresent()) {
            return recipeId;
        }

        RecipeIngredientEntity flour = RecipeIngredientEntity.builder()
                .ingredientLineId(UUID.randomUUID().toString())
                .recipeId(recipeId)
                .itemId(flourId)
                .itemName("Flour R5")
                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                .recipeQty(new BigDecimal("5.0"))
                .recipeUom("KG")
                .purchasingUnitSize(new BigDecimal("1"))
                .purchasingUom("KG")
                .wasteFactor(new BigDecimal("0"))
                .build();

        RecipeIngredientEntity sugar = RecipeIngredientEntity.builder()
                .ingredientLineId(UUID.randomUUID().toString())
                .recipeId(recipeId)
                .itemId(sugarId)
                .itemName("Sugar R5")
                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                .recipeQty(new BigDecimal("1.0"))
                .recipeUom("KG")
                .purchasingUnitSize(new BigDecimal("1"))
                .purchasingUom("KG")
                .wasteFactor(new BigDecimal("0"))
                .build();

        RecipeEntity recipe = RecipeEntity.builder()
                .recipeId(recipeId)
                .tenantId(TENANT)
                .productId(breadProductId)
                .versionNumber(1)
                .status(Recipe.RecipeStatus.ACTIVE)
                .batchSize(new BigDecimal("10"))
                .batchSizeUom("pcs")
                .expectedYield(new BigDecimal("10"))
                .yieldUom("pcs")
                .ingredients(List.of(flour, sugar))
                .build();

        recipeRepository.save(recipe);
        return recipeId;
    }

    private void seedStock(String itemId, BigDecimal qty, String uom, BigDecimal unitCost) {
        var event = com.breadcost.events.ReceiveLotEvent.builder()
                .tenantId(TENANT)
                .siteId("MAIN")
                .itemId(itemId)
                .lotId("LOT-" + itemId)
                .qty(qty)
                .uom(uom)
                .unitCostBase(unitCost)
                .occurredAtUtc(java.time.Instant.now())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        // Use event store to seed stock
        var eventStore = org.springframework.test.util.ReflectionTestUtils.getField(
                inventoryProjection, "eventStore");
        ((com.breadcost.eventstore.EventStore) eventStore)
                .appendEvent(event, com.breadcost.domain.LedgerEntry.EntryClass.FINANCIAL);
    }
}
