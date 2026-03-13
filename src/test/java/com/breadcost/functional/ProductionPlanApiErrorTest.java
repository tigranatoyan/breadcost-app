package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — ProductionPlan API error paths, RBAC gaps, and not-found.
 * Complements existing ProductionPlanFunctionalTest.
 */
@DisplayName("F2 :: ProductionPlan API — Error Paths & RBAC")
class ProductionPlanApiErrorTest extends FunctionalTestBase {

    // ── RBAC: approve requires Admin/Manager — test disallowed roles ──────────

    @Test
    @DisplayName("F2-PP-1 ✓ Warehouse cannot create plan (403)")
    void warehouse_createPlan_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "date", "2025-07-01", "shift", "MORNING");
        POST("/v1/production-plans", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PP-2 ✓ Technologist cannot create plan (403)")
    void technologist_createPlan_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "date", "2025-07-01", "shift", "MORNING");
        POST("/v1/production-plans", body, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PP-3 ✓ Warehouse cannot approve plan (403)")
    void warehouse_approvePlan_forbidden() throws Exception {
        POST_noBody("/v1/production-plans/fake-id/approve?tenantId=" + TENANT,
                bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PP-4 ✓ Technologist cannot approve plan (403)")
    void technologist_approvePlan_forbidden() throws Exception {
        POST_noBody("/v1/production-plans/fake-id/approve?tenantId=" + TENANT,
                bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PP-5 ✓ Cashier cannot publish plan (403)")
    void cashier_publishPlan_forbidden() throws Exception {
        POST_noBody("/v1/production-plans/fake-id/publish?tenantId=" + TENANT,
                bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── Not-found: operations on nonexistent plans ────────────────────────────

    @Test
    @DisplayName("F2-PP-6 ✓ Generate work orders for nonexistent plan returns 404")
    void generate_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/production-plans/does-not-exist/generate?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-PP-7 ✓ Approve nonexistent plan returns 404")
    void approve_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/production-plans/does-not-exist/approve?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-PP-8 ✓ Start nonexistent plan returns 404")
    void start_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/production-plans/does-not-exist/start?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-PP-9 ✓ Complete nonexistent plan returns 404")
    void complete_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/production-plans/does-not-exist/complete?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-PP-10 ✓ Material requirements for nonexistent plan returns 404")
    void materialRequirements_nonexistent_returns404() throws Exception {
        GET("/v1/production-plans/does-not-exist/material-requirements?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }
}
