package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Product &amp; Recipe API validation, RBAC, and not-found.
 * Complements existing ProductFunctionalTest and RecipeFunctionalTest.
 */
@DisplayName("F2 :: Product & Recipe — Validation, RBAC & Not-Found")
class ProductRecipeApiErrorTest extends FunctionalTestBase {

    // ── Product RBAC ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("F2-PRD-1 ✓ Warehouse cannot create product (403)")
    void warehouse_createProduct_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "departmentId", "dept1",
                "name", "Test Bread", "saleUnit", "PIECE", "baseUom", "pcs",
                "vatRatePct", 0.0);
        POST("/v1/products", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PRD-2 ✓ Finance cannot create product (403)")
    void finance_createProduct_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "departmentId", "dept1",
                "name", "Test Bread", "saleUnit", "PIECE", "baseUom", "pcs",
                "vatRatePct", 0.0);
        POST("/v1/products", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    // ── Product validation: @Valid CreateProductRequest ────────────────────────

    @Test
    @DisplayName("F2-PRD-3 ✓ Create product with blank name returns 400")
    void createProduct_blankName_returns400() throws Exception {
        var body = Map.of("tenantId", TENANT, "departmentId", "dept1",
                "name", "", "saleUnit", "PIECE", "baseUom", "pcs",
                "vatRatePct", 0.0);
        POST("/v1/products", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-PRD-4 ✓ Create product with blank tenantId returns 400")
    void createProduct_blankTenantId_returns400() throws Exception {
        var body = Map.of("tenantId", "", "departmentId", "dept1",
                "name", "Test Bread", "saleUnit", "PIECE", "baseUom", "pcs",
                "vatRatePct", 0.0);
        POST("/v1/products", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ── Product not-found ─────────────────────────────────────────────────────

    @Test
    @DisplayName("F2-PRD-5 ✓ Get nonexistent product returns error")
    void getProduct_nonexistent_returnsError() throws Exception {
        GET("/v1/products/does-not-exist", bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }

    // ── Recipe RBAC ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("F2-RCP-1 ✓ Warehouse cannot create recipe (403)")
    void warehouse_createRecipe_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "productId", "p1",
                "batchSize", 10, "batchSizeUom", "kg",
                "expectedYield", 9, "yieldUom", "kg",
                "ingredients", List.of());
        POST("/v1/recipes", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-RCP-2 ✓ Floor worker cannot create recipe (403)")
    void floorWorker_createRecipe_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "productId", "p1",
                "batchSize", 10, "batchSizeUom", "kg",
                "expectedYield", 9, "yieldUom", "kg",
                "ingredients", List.of());
        POST("/v1/recipes", body, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    // ── Recipe validation: @Valid CreateRecipeRequest ──────────────────────────

    @Test
    @DisplayName("F2-RCP-3 ✓ Create recipe with blank tenantId returns 400")
    void createRecipe_blankTenantId_returns400() throws Exception {
        var body = Map.of("tenantId", "", "productId", "p1",
                "batchSize", 10, "batchSizeUom", "kg",
                "expectedYield", 9, "yieldUom", "kg",
                "ingredients", List.of());
        POST("/v1/recipes", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-RCP-4 ✓ Create recipe with blank productId returns 400")
    void createRecipe_blankProductId_returns400() throws Exception {
        var body = Map.of("tenantId", TENANT, "productId", "",
                "batchSize", 10, "batchSizeUom", "kg",
                "expectedYield", 9, "yieldUom", "kg",
                "ingredients", List.of());
        POST("/v1/recipes", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ── Recipe not-found ──────────────────────────────────────────────────────

    @Test
    @DisplayName("F2-RCP-5 ✓ Get nonexistent recipe returns error")
    void getRecipe_nonexistent_returnsError() throws Exception {
        GET("/v1/recipes/does-not-exist?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }
}
