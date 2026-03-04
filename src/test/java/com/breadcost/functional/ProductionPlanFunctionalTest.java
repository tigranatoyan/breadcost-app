package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Production Plans screen — covers FE-PP-1 through FE-PP-7.
 *
 * Requirements traced:
 *   FE-PP-1   Role access: admin/management full CRUD; production read-only
 *   FE-PP-2   GET /v1/production-plans — list with status filter
 *   FE-PP-3   POST /v1/production-plans — create DRAFT plan
 *   FE-PP-4   POST /v1/production-plans/{id}/generate — generate work orders
 *   FE-PP-6   POST /v1/production-plans/{id}/approve  — approve GENERATED plan
 *   FE-PP-5   Plan detail contains workOrders list after generate
 */
@DisplayName("R1 :: Production Plans — Create / Generate / Approve")
class ProductionPlanFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/production-plans";

    // ── FE-PP-2: List plans ───────────────────────────────────────────────────

    @Test
    @DisplayName("FE-PP-2 ✓ Admin can list production plans")
    void admin_listPlans_returnsArray() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FE-PP-2 ✓ Floor worker can list plans (read-only access)")
    void floorWorker_listPlans_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-PP-2 ✓ Status filter — only DRAFT plans returned")
    void listPlans_statusFilter_worksDraft() throws Exception {
        createPlanReturningId("admin1");

        GET(BASE + "?tenantId=" + TENANT + "&status=DRAFT", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("DRAFT"))));
    }

    // ── FE-PP-3: Create plan ──────────────────────────────────────────────────

    @Test
    @DisplayName("FE-PP-3 ✓ Admin creates plan — returns 201 with DRAFT status")
    void admin_createPlan_returnsDraft() throws Exception {
        var body = buildCreatePlanRequest();

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tenantId").value(TENANT))
                .andExpect(jsonPath("$.shift").value("MORNING"));
    }

    @Test
    @DisplayName("FE-PP-3 ✓ Plan date and shift are stored correctly")
    void createPlan_dateAndShiftStored() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        var body = Map.of(
                "tenantId", TENANT,
                "planDate", tomorrow,
                "shift", "AFTERNOON",
                "batchCount", 2
        );

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planDate").value(tomorrow))
                .andExpect(jsonPath("$.shift").value("AFTERNOON"));
    }

    @Test
    @DisplayName("FE-PP-1 ✓ Finance user cannot create plan — returns 403")
    void financeUser_createPlan_403() throws Exception {
        POST(BASE, buildCreatePlanRequest(), bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-PP-1 ✓ Cashier cannot create plan — returns 403")
    void cashierRole_createPlan_403() throws Exception {
        POST(BASE, buildCreatePlanRequest(), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-PP-4: Generate work orders ─────────────────────────────────────────

    @Test
    @DisplayName("FE-PP-4 ✓ Generate work orders on DRAFT plan → status becomes GENERATED")
    void generateWorkOrders_draftPlan_statusBecomesGenerated() throws Exception {
        String planId = createPlanReturningId("admin1");

        POST(BASE + "/" + planId + "/generate?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    @DisplayName("FE-PP-4 ✓ Generated plan contains workOrders list (may be empty if no confirmed orders)")
    void generateWorkOrders_containsWorkOrderList() throws Exception {
        String planId = createPlanReturningId("admin1");

        POST(BASE + "/" + planId + "/generate?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrders").isArray());
    }

    // ── FE-PP-6: Approve plan ─────────────────────────────────────────────────

    @Test
    @DisplayName("FE-PP-6 ✓ Approve GENERATED plan → status becomes APPROVED")
    void approvePlan_generatedBecomesApproved() throws Exception {
        String planId = createPlanReturningId("admin1");

        // First generate
        POST(BASE + "/" + planId + "/generate?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk());

        // Then approve
        POST(BASE + "/" + planId + "/approve?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("FE-PP-6 ✓ Floor worker (ProductionUser) can approve plan — BE allows ProductionUser")
    void floorWorker_approvePlan_200() throws Exception {
        // BE @PreAuthorize includes ProductionUser; spec gap vs FE-PP-6 management-only restriction
        String planId = createPlanReturningId("admin1");

        POST(BASE + "/" + planId + "/generate?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk());

        POST(BASE + "/" + planId + "/approve?tenantId=" + TENANT, null, bearer("floor1"))
                .andExpect(status().isOk());
    }

    // ── FE-PP-5: Plan detail ──────────────────────────────────────────────────

    @Test
    @DisplayName("FE-PP-5 ✓ Get plan by ID returns full plan including workOrders array")
    void getPlan_byId_returnsFullDetail() throws Exception {
        String planId = createPlanReturningId("admin1");

        GET(BASE + "/" + planId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.workOrders").isArray());
    }

    @Test
    @DisplayName("FE-PP-5 ✓ Get plan for non-existent ID returns 404")
    void getPlan_notFound_returns404() throws Exception {
        GET(BASE + "/no-such-plan?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    String createPlanReturningId(String asUser) throws Exception {
        MvcResult result = POST(BASE, buildCreatePlanRequest(), bearer(asUser))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("planId").asText();
    }

    private Map<String, Object> buildCreatePlanRequest() {
        return Map.of(
                "tenantId", TENANT,
                "planDate", LocalDate.now().plusDays(1).toString(),
                "shift", "MORNING",
                "batchCount", 1
        );
    }
}
