package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Recipe management — covers FR-4.1 through FR-4.7.
 *
 * Requirements traced:
 *   FR-4.1   Create new recipe version (DRAFT) with ingredients
 *   FR-4.2   Activate recipe → archives previous active version
 *   FR-4.3   Version history list for a product
 *   FR-4.4   Material requirements calculation (purchasing units)
 *   FR-4.5   Batch multiplier scales material requirements
 *   FR-4.6   Update ingredients on a DRAFT recipe
 *   FR-4.7   Only Technologist/Admin can create/activate recipes
 */
@DisplayName("R1 :: Recipes — CRUD, Versioning & Activation")
class RecipeFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/recipes";
    private String deptId;

    @BeforeEach
    void setupDepartment() throws Exception {
        var deptBody = Map.of(
                "tenantId", TENANT,
                "name", "RecipeDept " + System.nanoTime(),
                "leadTimeHours", 8,
                "warehouseMode", "SHARED"
        );
        MvcResult result = POST("/v1/departments", deptBody, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        deptId = om.readTree(result.getResponse().getContentAsString())
                .get("departmentId").asText();
    }

    /** Create a real product and return its productId. */
    private String createProductId() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "departmentId", deptId,
                "name", "Prod-" + System.nanoTime(),
                "saleUnit", "PIECE",
                "baseUom", "pcs",
                "price", 5000,
                "vatRatePct", 12.0
        );
        MvcResult r = POST("/v1/products", body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString())
                .get("productId").asText();
    }

    // ── FR-4.1: Create recipe ─────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.1 ✓ Technologist creates DRAFT recipe — returns 201 with recipeId")
    void tech_createRecipe_returnsDraft() throws Exception {
        String productId = createProductId();
        POST(BASE, buildRecipeRequest(productId), bearer("tech1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipeId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.productId").value(productId));
    }

    @Test
    @DisplayName("FR-4.1 ✓ Recipe ingredients stored correctly")
    void createRecipe_ingredientsInResponse() throws Exception {
        String productId = createProductId();
        POST(BASE, buildRecipeRequest(productId), bearer("tech1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients").isArray())
                .andExpect(jsonPath("$.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.ingredients[0].itemId").value("flour-001"));
    }

    @Test
    @DisplayName("FR-4.1 ✓ Admin can also create recipes")
    void admin_createRecipe_succeeds() throws Exception {
        String productId = createProductId();
        POST(BASE, buildRecipeRequest(productId), bearer("admin1"))
                .andExpect(status().isCreated());
    }

    // ── FR-4.7: Role enforcement ──────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.7 ✓ Cashier cannot create recipe — 403")
    void cashier_createRecipe_forbidden() throws Exception {
        String productId = createProductId();
        POST(BASE, buildRecipeRequest(productId), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-4.7 ✓ Floor worker cannot create recipe — 403")
    void floor_createRecipe_forbidden() throws Exception {
        String productId = createProductId();
        POST(BASE, buildRecipeRequest(productId), bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    // ── FR-4.2: Activate recipe ───────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.2 ✓ Activate DRAFT recipe → status becomes ACTIVE")
    void activateRecipe_becomesActive() throws Exception {
        String recipeId = createRecipeReturningId(createProductId(), "tech1");

        POST_noBody(BASE + "/" + recipeId + "/activate?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("FR-4.2 ✓ Activating new version archives previous active recipe")
    void activateRecipe_archivesPrevious() throws Exception {
        String productId = createProductId();

        // Create and activate version 1
        String v1 = createRecipeReturningId(productId, "tech1");
        POST_noBody(BASE + "/" + v1 + "/activate?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk());

        // Create and activate version 2
        String v2 = createRecipeReturningId(productId, "tech1");
        POST_noBody(BASE + "/" + v2 + "/activate?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk());

        // Version 1 should now be ARCHIVED
        GET(BASE + "/" + v1 + "?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    // ── FR-4.3: Version history ───────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ List recipe versions for a product returns all versions")
    void listVersionHistory_returnsAll() throws Exception {
        String productId = createProductId();

        createRecipeReturningId(productId, "tech1");
        createRecipeReturningId(productId, "tech1");

        GET(BASE + "?tenantId=" + TENANT + "&productId=" + productId, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("FR-4.3 ✓ Manager can read recipe version history")
    void manager_listVersionHistory_succeeds() throws Exception {
        String productId = createProductId();
        createRecipeReturningId(productId, "tech1");

        GET(BASE + "?tenantId=" + TENANT + "&productId=" + productId, bearer("manager1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── FR-4.2: Get active recipe ─────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.2 ✓ Get active recipe for a product returns the activated version")
    void getActiveRecipe_returnsActive() throws Exception {
        String productId = createProductId();
        String recipeId = createRecipeReturningId(productId, "tech1");

        POST_noBody(BASE + "/" + recipeId + "/activate?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk());

        GET(BASE + "/active?tenantId=" + TENANT + "&productId=" + productId, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(recipeId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── FR-4.6: Update ingredients ────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.6 ✓ Update ingredients on DRAFT recipe — full replacement")
    void updateIngredients_onDraft_succeeds() throws Exception {
        String recipeId = createRecipeReturningId(createProductId(), "tech1");

        List<Map<String, Object>> newIngredients = List.of(
                Map.of(
                        "itemId", "sugar-001",
                        "itemName", "Sugar",
                        "unitMode", "WEIGHT",
                        "recipeQty", 500,
                        "recipeUom", "G",
                        "purchasingUnitSize", 1000,
                        "purchasingUom", "G"
                )
        );

        PUT(BASE + "/" + recipeId + "/ingredients?tenantId=" + TENANT,
                newIngredients, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.ingredients[0].itemId").value("sugar-001"));
    }

    // ── FR-4.4 / FR-4.5: Material requirements ───────────────────────────────

    @Test
    @DisplayName("FR-4.4 ✓ Material requirements calculation returns purchasing units")
    void materialRequirements_returnsPurchasingUnits() throws Exception {
        String recipeId = createRecipeReturningId(createProductId(), "tech1");

        GET(BASE + "/" + recipeId + "/material-requirements?tenantId=" + TENANT
                        + "&batchMultiplier=1", bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].itemId").value("flour-001"));
    }

    @Test
    @DisplayName("FR-4.5 ✓ Batch multiplier scales material requirements")
    void materialRequirements_batchMultiplier_scales() throws Exception {
        String recipeId = createRecipeReturningId(createProductId(), "tech1");

        // Get for 1 batch
        MvcResult r1 = GET(BASE + "/" + recipeId + "/material-requirements?tenantId=" + TENANT
                        + "&batchMultiplier=1", bearer("tech1"))
                .andExpect(status().isOk())
                .andReturn();

        // Get for 3 batches
        MvcResult r3 = GET(BASE + "/" + recipeId + "/material-requirements?tenantId=" + TENANT
                        + "&batchMultiplier=3", bearer("tech1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items1 = om.readTree(r1.getResponse().getContentAsString());
        JsonNode items3 = om.readTree(r3.getResponse().getContentAsString());

        // 3x batch should require more purchasing units than 1x
        double pu1 = items1.get(0).get("purchasingUnitsNeeded").asDouble();
        double pu3 = items3.get(0).get("purchasingUnitsNeeded").asDouble();
        org.junit.jupiter.api.Assertions.assertTrue(pu3 > pu1,
                "3 batches should need more purchasing units than 1 batch");
    }

    // ── Get single recipe ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.1 ✓ Get single recipe by ID returns correct data")
    void getRecipe_byId_returnsCorrect() throws Exception {
        String recipeId = createRecipeReturningId(createProductId(), "tech1");

        GET(BASE + "/" + recipeId + "?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(recipeId))
                .andExpect(jsonPath("$.batchSize").value(10));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String createRecipeReturningId(String productId, String asUser) throws Exception {
        MvcResult result = POST(BASE, buildRecipeRequest(productId), bearer(asUser))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("recipeId").asText();
    }

    private Map<String, Object> buildRecipeRequest(String productId) {
        return Map.of(
                "tenantId", TENANT,
                "productId", productId,
                "batchSize", 10,
                "batchSizeUom", "KG",
                "expectedYield", 9,
                "yieldUom", "KG",
                "productionNotes", "Test recipe",
                "leadTimeHours", 4,
                "ingredients", List.of(
                        Map.of(
                                "itemId", "flour-001",
                                "itemName", "Wheat Flour",
                                "unitMode", "WEIGHT",
                                "recipeQty", 5000,
                                "recipeUom", "G",
                                "purchasingUnitSize", 25000,
                                "purchasingUom", "G"
                        )
                )
        );
    }
}
