package com.breadcost;

import com.breadcost.domain.Department;
import com.breadcost.domain.Product;
import com.breadcost.domain.Recipe;
import com.breadcost.domain.RecipeIngredient;
import com.breadcost.masterdata.*;
import com.breadcost.reporting.ReportService;
import com.breadcost.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the database with demo data for manual testing.
 * Active by default (no profile restriction) — safe because H2 uses "update" ddl-auto.
 * Uses insertIfMissing logic to be idempotent across restarts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEPT_BREAD = "DEPT-BREAD";
    private static final String INGREDIENT_TYPE = "INGREDIENT";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantConfigRepository tenantConfigRepository;
    private final ReportService reportService;
    private final SubscriptionService subscriptionService;

    private static final String TENANT = "tenant1";

    @Override
    public void run(String... args) {
        // Always seed catalogs (methods are idempotent)
        reportService.seedKpiBlocks();
        subscriptionService.seedTiers();

        // Ensure demo tenant has ENTERPRISE tier for all features (idempotent)
        subscriptionService.assignTier(TENANT, "ENTERPRISE", "system", java.time.LocalDate.now(), null);

        // Always seed recipes if missing (idempotent)
        seedRecipesIfMissing();

        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("Seed data already exists — skipping");
            return;
        }
        log.info("Seeding demo data for tenant: {}", TENANT);

        // ── Tenant config ────────────────────────────────────────────
        tenantConfigRepository.save(TenantConfigEntity.builder()
                .tenantId(TENANT)
                .displayName("BreadCost Demo Bakery")
                .orderCutoffTime("22:00")
                .rushOrderPremiumPct(15.0)
                .mainCurrency("AMD")
                .build());

        // ── Users (7 roles) ──────────────────────────────────────────
        seedUser("admin",       "Admin User",       "Admin");
        seedUser("manager",     "Manager User",     "Manager");
        seedUser("technologist","Technologist User", "Technologist");
        seedUser("production",  "Floor Worker",     "ProductionUser");
        seedUser("finance",     "Finance User",     "FinanceUser");
        seedUser("warehouse",   "Warehouse User",   "Warehouse");
        seedUser("cashier",     "Cashier User",     "Cashier");

        // ── Departments ──────────────────────────────────────────────
        seedDept(DEPT_BREAD,       "Bread Department",    6);
        seedDept("DEPT-PASTRY",  "Pastry Department",   8);
        seedDept("DEPT-CONFECT", "Confectionery Dept", 12);

        // ── Items (ingredients) ──────────────────────────────────────
        seedItem("ITEM-FLOUR",  "Wheat Flour",    INGREDIENT_TYPE, "KG", 50);
        seedItem("ITEM-SUGAR",  "White Sugar",    INGREDIENT_TYPE, "KG", 30);
        seedItem("ITEM-BUTTER", "Butter",         INGREDIENT_TYPE, "KG", 20);
        seedItem("ITEM-YEAST",  "Dry Yeast",      INGREDIENT_TYPE, "KG",  5);
        seedItem("ITEM-SALT",   "Table Salt",     INGREDIENT_TYPE, "KG", 10);
        seedItem("ITEM-EGGS",   "Eggs",           INGREDIENT_TYPE, "PCS", 100);
        seedItem("ITEM-MILK",   "Whole Milk",     INGREDIENT_TYPE, "L",  25);

        // ── Products ─────────────────────────────────────────────────
        seedProduct("PROD-WHITE",    "White Bread",     DEPT_BREAD,     Product.SaleUnit.PIECE, 8000);
        seedProduct("PROD-SOUR",     "Sourdough Bread", DEPT_BREAD,     Product.SaleUnit.PIECE, 15000);
        seedProduct("PROD-BAGUETTE", "Baguette",        DEPT_BREAD,     Product.SaleUnit.PIECE, 12000);
        seedProduct("PROD-CROISS",   "Croissant",       "DEPT-PASTRY", Product.SaleUnit.PIECE, 10000);
        seedProduct("PROD-CAKE",     "Birthday Cake",   "DEPT-CONFECT",Product.SaleUnit.PIECE, 85000);

        // ── Recipes (1 active recipe per product) ────────────────────
        seedRecipe("RCP-WHITE",    "PROD-WHITE",    20, 6,
                List.of(ing("ITEM-FLOUR", "Wheat Flour", 5.0), ing("ITEM-YEAST", "Dry Yeast", 0.1), ing("ITEM-SALT", "Table Salt", 0.1)));
        seedRecipe("RCP-SOUR",     "PROD-SOUR",     15, 8,
                List.of(ing("ITEM-FLOUR", "Wheat Flour", 6.0), ing("ITEM-SALT", "Table Salt", 0.12)));
        seedRecipe("RCP-BAGUETTE", "PROD-BAGUETTE", 25, 5,
                List.of(ing("ITEM-FLOUR", "Wheat Flour", 4.0), ing("ITEM-YEAST", "Dry Yeast", 0.08), ing("ITEM-SALT", "Table Salt", 0.08)));
        seedRecipe("RCP-CROISS",   "PROD-CROISS",   30, 8,
                List.of(ing("ITEM-FLOUR", "Wheat Flour", 3.0), ing("ITEM-BUTTER", "Butter", 2.0), ing("ITEM-EGGS", "Eggs", 0.5)));
        seedRecipe("RCP-CAKE",     "PROD-CAKE",      5, 12,
                List.of(ing("ITEM-FLOUR", "Wheat Flour", 2.0), ing("ITEM-SUGAR", "White Sugar", 1.5),
                        ing("ITEM-BUTTER", "Butter", 1.0), ing("ITEM-EGGS", "Eggs", 1.0), ing("ITEM-MILK", "Whole Milk", 0.5)));

        log.info("Demo data seeded successfully: 7 users, 3 departments, 7 items, 5 products, 5 recipes");
    }

    private void seedUser(String username, String displayName, String role) {
        userRepository.save(UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .tenantId(TENANT)
                .username(username)
                .passwordHash(passwordEncoder.encode(username))  // password = username
                .displayName(displayName)
                .roles(role)
                .active(true)
                .build());
    }

    private void seedDept(String id, String name, int leadTimeHours) {
        departmentRepository.save(DepartmentEntity.builder()
                .departmentId(id)
                .tenantId(TENANT)
                .name(name)
                .leadTimeHours(leadTimeHours)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .build());
    }

    private void seedItem(String id, String name, String type, String uom, double minThreshold) {
        itemRepository.save(ItemEntity.builder()
                .itemId(id)
                .tenantId(TENANT)
                .name(name)
                .type(type)
                .baseUom(uom)
                .minStockThreshold(minThreshold)
                .active(true)
                .build());
    }

    private void seedProduct(String id, String name, String deptId, Product.SaleUnit saleUnit, double price) {
        productRepository.save(ProductEntity.builder()
                .productId(id)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name(name)
                .saleUnit(saleUnit)
                .baseUom(saleUnit == Product.SaleUnit.WEIGHT ? "KG" : "PCS")
                .price(BigDecimal.valueOf(price))
                .vatRatePct(12.0)
                .status(Product.ProductStatus.ACTIVE)
                .build());
    }

    private RecipeIngredientEntity ing(String itemId, String itemName, double qty) {
        return RecipeIngredientEntity.builder()
                .ingredientLineId(UUID.randomUUID().toString())
                .tenantId(TENANT)
                .itemId(itemId)
                .itemName(itemName)
                .unitMode(RecipeIngredient.UnitMode.WEIGHT)
                .recipeQty(BigDecimal.valueOf(qty))
                .recipeUom("KG")
                .purchasingUnitSize(BigDecimal.ONE)
                .purchasingUom("KG")
                .wasteFactor(BigDecimal.valueOf(0.05))
                .build();
    }

    private void seedRecipe(String id, String productId, int batchSize, int leadTimeHours,
                            List<RecipeIngredientEntity> ingredients) {
        RecipeEntity recipe = RecipeEntity.builder()
                .recipeId(id)
                .tenantId(TENANT)
                .productId(productId)
                .versionNumber(1)
                .status(Recipe.RecipeStatus.ACTIVE)
                .batchSize(BigDecimal.valueOf(batchSize))
                .batchSizeUom("PCS")
                .expectedYield(BigDecimal.valueOf(batchSize))
                .yieldUom("PCS")
                .leadTimeHours(leadTimeHours)
                .productionNotes("Standard recipe")
                .createdBy("system")
                .ingredients(new ArrayList<>())
                .build();
        ingredients.forEach(i -> {
            i.setRecipeId(id);
            recipe.getIngredients().add(i);
        });
        recipeRepository.save(recipe);
    }

    private void seedRecipesIfMissing() {
        if (recipeRepository.findByTenantIdAndProductIdAndStatus(TENANT, "PROD-WHITE", Recipe.RecipeStatus.ACTIVE).isEmpty()) {
            log.info("Seeding missing recipes for tenant: {}", TENANT);
            seedRecipe("RCP-WHITE",    "PROD-WHITE",    20, 6,
                    List.of(ing("ITEM-FLOUR", "Wheat Flour", 5.0), ing("ITEM-YEAST", "Dry Yeast", 0.1), ing("ITEM-SALT", "Table Salt", 0.1)));
            seedRecipe("RCP-SOUR",     "PROD-SOUR",     15, 8,
                    List.of(ing("ITEM-FLOUR", "Wheat Flour", 6.0), ing("ITEM-SALT", "Table Salt", 0.12)));
            seedRecipe("RCP-BAGUETTE", "PROD-BAGUETTE", 25, 5,
                    List.of(ing("ITEM-FLOUR", "Wheat Flour", 4.0), ing("ITEM-YEAST", "Dry Yeast", 0.08), ing("ITEM-SALT", "Table Salt", 0.08)));
            seedRecipe("RCP-CROISS",   "PROD-CROISS",   30, 8,
                    List.of(ing("ITEM-FLOUR", "Wheat Flour", 3.0), ing("ITEM-BUTTER", "Butter", 2.0), ing("ITEM-EGGS", "Eggs", 0.5)));
            seedRecipe("RCP-CAKE",     "PROD-CAKE",      5, 12,
                    List.of(ing("ITEM-FLOUR", "Wheat Flour", 2.0), ing("ITEM-SUGAR", "White Sugar", 1.5),
                            ing("ITEM-BUTTER", "Butter", 1.0), ing("ITEM-EGGS", "Eggs", 1.0), ing("ITEM-MILK", "Whole Milk", 0.5)));
        }
    }
}
