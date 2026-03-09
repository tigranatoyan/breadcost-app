package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Product management — covers FR-4.1, FR-4.7, FR-4.8.
 *
 * Requirements traced:
 *   FR-4.1   Create a new product in a department
 *   FR-4.7   Only Technologist/Admin can create/update products
 *   FR-4.8   Product has sale unit (PIECE/WEIGHT/BOTH), base UOM, price, status
 */
@DisplayName("R1 :: Products — CRUD, Sale Unit & Status")
class ProductFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/products";
    private String deptId;

    @BeforeEach
    void createDepartment() throws Exception {
        var deptBody = Map.of(
                "tenantId", TENANT,
                "name", "Test Dept " + System.nanoTime(),
                "leadTimeHours", 8,
                "warehouseMode", "SHARED"
        );
        MvcResult result = POST("/v1/departments", deptBody, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        deptId = node.get("departmentId").asText();
    }

    // ── FR-4.1: Create product ────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.1 ✓ Admin creates product — returns 201 with productId")
    void admin_createProduct_returns201() throws Exception {
        POST(BASE, buildCreateRequest("White Bread"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("White Bread"))
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }

    @Test
    @DisplayName("FR-4.1 ✓ Technologist creates product — returns 201")
    void tech_createProduct_returns201() throws Exception {
        POST(BASE, buildCreateRequest("Sourdough"), bearer("tech1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sourdough"));
    }

    // ── FR-4.7: Role enforcement ──────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.7 ✓ Cashier cannot create product — 403")
    void cashier_createProduct_forbidden() throws Exception {
        POST(BASE, buildCreateRequest("Forbidden Bread"), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-4.7 ✓ Manager cannot create product — 403")
    void manager_createProduct_forbidden() throws Exception {
        POST(BASE, buildCreateRequest("No Access"), bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    // ── FR-4.8: Sale unit, price, status ──────────────────────────────────────

    @Test
    @DisplayName("FR-4.8 ✓ Product created with PIECE sale unit is reflected in response")
    void createProduct_pieceSaleUnit_reflected() throws Exception {
        POST(BASE, buildCreateRequest("Baguette"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleUnit").value("PIECE"))
                .andExpect(jsonPath("$.baseUom").value("pcs"));
    }

    @Test
    @DisplayName("FR-4.8 ✓ Product price and VAT rate stored correctly")
    void createProduct_priceAndVat_stored() throws Exception {
        POST(BASE, buildCreateRequest("Croissant"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(5000))
                .andExpect(jsonPath("$.vatRatePct").value(12.0));
    }

    // ── List products ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.1 ✓ List products by tenant returns array")
    void listProducts_byTenant_returnsArray() throws Exception {
        createProductReturningId("List Item A");

        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FR-4.1 ✓ List products filtered by departmentId")
    void listProducts_byDepartment_filtered() throws Exception {
        GET(BASE + "?tenantId=" + TENANT + "&departmentId=dept-none", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FR-4.1 ✓ Manager can read product list")
    void manager_listProducts_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("manager1"))
                .andExpect(status().isOk());
    }

    // ── Get single product ────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.1 ✓ Get product by ID returns correct data")
    void getProduct_byId_returnsCorrect() throws Exception {
        String productId = createProductReturningId("Get Test");

        GET(BASE + "/" + productId, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.name").value("Get Test"));
    }

    // ── Update product ────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-4.8 ✓ Update product name and status")
    void updateProduct_nameAndStatus() throws Exception {
        String productId = createProductReturningId("Old Name");

        var updateBody = Map.of(
                "name", "New Name",
                "description", "Updated desc",
                "saleUnit", "WEIGHT",
                "baseUom", "kg",
                "price", 7500,
                "vatRatePct", 10.0,
                "status", "ACTIVE"
        );

        PUT(BASE + "/" + productId, updateBody, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.saleUnit").value("WEIGHT"));
    }

    @Test
    @DisplayName("FR-4.8 ✓ Discontinue a product via status update")
    void updateProduct_discontinue() throws Exception {
        String productId = createProductReturningId("To Discontinue");

        var updateBody = Map.of(
                "name", "To Discontinue",
                "description", "",
                "saleUnit", "PIECE",
                "baseUom", "pcs",
                "price", 5000,
                "vatRatePct", 12.0,
                "status", "DISCONTINUED"
        );

        PUT(BASE + "/" + productId, updateBody, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String createProductReturningId(String name) throws Exception {
        MvcResult result = POST(BASE, buildCreateRequest(name), bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("productId").asText();
    }

    private Map<String, Object> buildCreateRequest(String name) {
        return Map.of(
                "tenantId", TENANT,
                "departmentId", deptId,
                "name", name,
                "description", "Test product",
                "saleUnit", "PIECE",
                "baseUom", "pcs",
                "price", 5000,
                "vatRatePct", 12.0
        );
    }
}
