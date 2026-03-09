package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Technology Steps per Recipe.
 *
 * Requirements traced:
 *   FR-4.3   Technology steps define production process per recipe
 *   FR-4.3   Each step has: number, name, activities, instruments, duration, temperature
 *   FR-4.3   Steps are ordered and linked to a recipe
 */
@DisplayName("R1 :: Technology Steps — CRUD per Recipe")
class TechnologyStepFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/technology-steps";
    private static final String RECIPE_BASE = "/v1/recipes";

    // ── Create step ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ Admin creates technology step — returns 201")
    void admin_createStep_returns201() throws Exception {
        String recipeId = createRecipeReturningId();

        POST(BASE + "?tenantId=" + TENANT,
                buildStepRequest(recipeId, 1, "Mixing"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stepId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Mixing"))
                .andExpect(jsonPath("$.stepNumber").value(1));
    }

    @Test
    @DisplayName("FR-4.3 ✓ Floor worker (ProductionUser) creates step — returns 201")
    void floor_createStep_returns201() throws Exception {
        String recipeId = createRecipeReturningId();

        POST(BASE + "?tenantId=" + TENANT,
                buildStepRequest(recipeId, 1, "Kneading"), bearer("floor1"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("FR-4.3 ✓ Step stores duration and temperature")
    void createStep_durationAndTemperature_stored() throws Exception {
        String recipeId = createRecipeReturningId();

        POST(BASE + "?tenantId=" + TENANT,
                buildStepRequest(recipeId, 1, "Baking"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.temperatureCelsius").value(220));
    }

    // ── List steps ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ List steps for a recipe returns ordered array")
    void listSteps_returnsArray() throws Exception {
        String recipeId = createRecipeReturningId();
        createStepReturningId(recipeId, 1, "Step One");
        createStepReturningId(recipeId, 2, "Step Two");

        GET(BASE + "?tenantId=" + TENANT + "&recipeId=" + recipeId, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ── Update step ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ Update technology step name and duration")
    void updateStep_nameAndDuration() throws Exception {
        String recipeId = createRecipeReturningId();
        String stepId = createStepReturningId(recipeId, 1, "Old Step");

        var updateBody = Map.of(
                "recipeId", recipeId,
                "stepNumber", 1,
                "name", "Updated Step",
                "activities", "Revised mixing procedure",
                "instruments", "New mixer model",
                "durationMinutes", 60,
                "temperatureCelsius", 200
        );

        PUT(BASE + "/" + stepId + "?tenantId=" + TENANT, updateBody, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Step"))
                .andExpect(jsonPath("$.durationMinutes").value(60));
    }

    // ── Delete step ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ Delete technology step — returns 204")
    void deleteStep_returns204() throws Exception {
        String recipeId = createRecipeReturningId();
        String stepId = createStepReturningId(recipeId, 1, "To Delete");

        DELETE(BASE + "/" + stepId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNoContent());

        // Verify it's gone
        GET(BASE + "?tenantId=" + TENANT + "&recipeId=" + recipeId, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Role enforcement ──────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.3 ✓ Cashier cannot create technology step — 403")
    void cashier_createStep_forbidden() throws Exception {
        String recipeId = createRecipeReturningId();

        POST(BASE + "?tenantId=" + TENANT,
                buildStepRequest(recipeId, 1, "Forbidden"), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String createRecipeReturningId() throws Exception {
        // Create a real department + product first
        var deptBody = Map.of(
                "tenantId", TENANT,
                "name", "StepDept " + System.nanoTime(),
                "leadTimeHours", 8,
                "warehouseMode", "SHARED"
        );
        MvcResult deptResult = POST("/v1/departments", deptBody, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        String deptId = om.readTree(deptResult.getResponse().getContentAsString())
                .get("departmentId").asText();

        var prodBody = Map.of(
                "tenantId", TENANT,
                "departmentId", deptId,
                "name", "StepProd " + System.nanoTime(),
                "saleUnit", "PIECE",
                "baseUom", "pcs",
                "price", 5000,
                "vatRatePct", 12.0
        );
        MvcResult prodResult = POST("/v1/products", prodBody, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        String productId = om.readTree(prodResult.getResponse().getContentAsString())
                .get("productId").asText();

        var recipeBody = Map.of(
                "tenantId", TENANT,
                "productId", productId,
                "batchSize", 10,
                "batchSizeUom", "KG",
                "expectedYield", 9,
                "yieldUom", "KG",
                "productionNotes", "Test recipe for steps",
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

        MvcResult result = POST(RECIPE_BASE, recipeBody, bearer("tech1"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("recipeId").asText();
    }

    private String createStepReturningId(String recipeId, int stepNumber, String name) throws Exception {
        MvcResult result = POST(BASE + "?tenantId=" + TENANT,
                buildStepRequest(recipeId, stepNumber, name), bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("stepId").asText();
    }

    private Map<String, Object> buildStepRequest(String recipeId, int stepNumber, String name) {
        return Map.of(
                "recipeId", recipeId,
                "stepNumber", stepNumber,
                "name", name,
                "activities", "Mix all ingredients thoroughly",
                "instruments", "Industrial mixer",
                "durationMinutes", 45,
                "temperatureCelsius", 220
        );
    }
}
