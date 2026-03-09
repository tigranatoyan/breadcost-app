package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Department management — covers FR-11.1, FR-11.2.
 *
 * Requirements traced:
 *   FR-11.1   Department CRUD — up to 10 per tenant
 *   FR-11.2   Department has lead time, warehouse mode (SHARED/ISOLATED), status
 */
@DisplayName("R1 :: Departments — CRUD, Lead Time & Warehouse Mode")
class DepartmentFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/departments";

    // ── FR-11.1: Create department ────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.1 ✓ Admin creates department — returns 201")
    void admin_createDepartment_returns201() throws Exception {
        POST(BASE, buildCreateRequest("Bakery A"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departmentId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Bakery A"))
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }

    @Test
    @DisplayName("FR-11.1 ✓ Only Admin can create departments — Manager gets 403")
    void manager_createDepartment_forbidden() throws Exception {
        POST(BASE, buildCreateRequest("Forbidden Dept"), bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-11.1 ✓ Only Admin can create departments — Technologist gets 403")
    void tech_createDepartment_forbidden() throws Exception {
        POST(BASE, buildCreateRequest("Forbidden Dept"), bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    // ── FR-11.2: Lead time and warehouse mode ─────────────────────────────────

    @Test
    @DisplayName("FR-11.2 ✓ Department created with SHARED warehouse mode and lead time")
    void createDepartment_sharedMode_leadTime() throws Exception {
        POST(BASE, buildCreateRequest("Shared Dept"), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseMode").value("SHARED"))
                .andExpect(jsonPath("$.leadTimeHours").value(8));
    }

    @Test
    @DisplayName("FR-11.2 ✓ Department with ISOLATED warehouse mode")
    void createDepartment_isolatedMode() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "name", "Isolated Dept",
                "leadTimeHours", 12,
                "warehouseMode", "ISOLATED"
        );

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseMode").value("ISOLATED"))
                .andExpect(jsonPath("$.leadTimeHours").value(12));
    }

    // ── List departments ──────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.1 ✓ List departments by tenant returns array")
    void listDepartments_returnsArray() throws Exception {
        createDepartmentReturningId("List Dept");

        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FR-11.1 ✓ Manager can read department list")
    void manager_listDepartments_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("manager1"))
                .andExpect(status().isOk());
    }

    // ── Get single department ─────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.1 ✓ Get department by ID returns correct data")
    void getDepartment_byId_returnsCorrect() throws Exception {
        String deptId = createDepartmentReturningId("Get Dept");

        GET(BASE + "/" + deptId, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(deptId))
                .andExpect(jsonPath("$.name").value("Get Dept"));
    }

    // ── Update department ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.2 ✓ Update department lead time and warehouse mode")
    void updateDepartment_leadTimeAndMode() throws Exception {
        String deptId = createDepartmentReturningId("Update Dept");

        var updateBody = Map.of(
                "name", "Updated Dept",
                "leadTimeHours", 24,
                "warehouseMode", "ISOLATED",
                "status", "ACTIVE"
        );

        PUT(BASE + "/" + deptId, updateBody, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Dept"))
                .andExpect(jsonPath("$.leadTimeHours").value(24))
                .andExpect(jsonPath("$.warehouseMode").value("ISOLATED"));
    }

    @Test
    @DisplayName("FR-11.2 ✓ Deactivate a department via status update")
    void updateDepartment_deactivate() throws Exception {
        String deptId = createDepartmentReturningId("Deactivate Dept");

        var updateBody = Map.of(
                "name", "Deactivate Dept",
                "leadTimeHours", 8,
                "warehouseMode", "SHARED",
                "status", "INACTIVE"
        );

        PUT(BASE + "/" + deptId, updateBody, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("FR-11.1 ✓ Manager cannot update department — 403")
    void manager_updateDepartment_forbidden() throws Exception {
        String deptId = createDepartmentReturningId("NoUpdate Dept");

        var updateBody = Map.of(
                "name", "Hacked Dept",
                "leadTimeHours", 1,
                "warehouseMode", "SHARED",
                "status", "ACTIVE"
        );

        PUT(BASE + "/" + deptId, updateBody, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String createDepartmentReturningId(String name) throws Exception {
        MvcResult result = POST(BASE, buildCreateRequest(name), bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("departmentId").asText();
    }

    private Map<String, Object> buildCreateRequest(String name) {
        return Map.of(
                "tenantId", TENANT,
                "name", name,
                "leadTimeHours", 8,
                "warehouseMode", "SHARED"
        );
    }
}
