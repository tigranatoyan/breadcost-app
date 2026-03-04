package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Reports screen — covers FE-RPT-1, FE-RPT-2, FE-DASH-2.
 *
 * Requirements traced:
 *   FE-RPT-1   Role access: admin, management, finance for /reports
 *   FE-RPT-2   GET /v1/reports/revenue-summary — today/week/month/allTime + currency
 *   FE-RPT-2   GET /v1/reports/top-products   — list of top products by qty in last 7 days
 *   FE-DASH-2  Revenue widgets on dashboard sourced from revenue-summary endpoint
 *   NFR-FE-3   Report endpoints work without errors on empty data
 */
@DisplayName("R1 :: Reports — Revenue Summary & Top Products")
class ReportsFunctionalTest extends FunctionalTestBase {

    // ── FE-RPT-1: access control ──────────────────────────────────────────────

    @Test
    @DisplayName("FE-RPT-1 ✓ Admin can access revenue summary")
    void admin_revenueSummary_succeeds() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-RPT-1 ✓ Finance user can access revenue summary")
    void finance_revenueSummary_succeeds() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-RPT-1 ✓ Cashier cannot access revenue summary — returns 403")
    void cashier_revenueSummary_403() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-RPT-1 ✓ Floor worker cannot access revenue summary — returns 403")
    void floorWorker_revenueSummary_403() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-RPT-2: revenue summary shape ───────────────────────────────────────

    @Test
    @DisplayName("FE-RPT-2 ✓ Revenue summary response has required fields")
    void revenueSummary_responseShape() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").exists())
                .andExpect(jsonPath("$.week").exists())
                .andExpect(jsonPath("$.month").exists())
                .andExpect(jsonPath("$.allTime").exists())
                .andExpect(jsonPath("$.currency").value("UZS"));
    }

    @Test
    @DisplayName("FE-RPT-2 ✓ Revenue summary with no orders — all amounts are 0")
    void revenueSummary_noOrders_allZero() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=new-empty-tenant", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value(0))
                .andExpect(jsonPath("$.week").value(0))
                .andExpect(jsonPath("$.allTime").value(0));
    }

    @Test
    @DisplayName("FE-RPT-2 ✓ Revenue summary period param accepted (month default)")
    void revenueSummary_periodParam_accepted() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT + "&period=week", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("UZS"));
    }

    // ── FE-RPT-2: top products shape ──────────────────────────────────────────

    @Test
    @DisplayName("FE-RPT-2 ✓ Top products returns array")
    void topProducts_returnsArray() throws Exception {
        GET("/v1/reports/top-products?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FE-RPT-2 ✓ Top products respects limit parameter")
    void topProducts_limitParam_respected() throws Exception {
        GET("/v1/reports/top-products?tenantId=" + TENANT + "&limit=3", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("FE-RPT-2 ✓ Top products items have required fields when data exists")
    void topProducts_withData_itemsHaveRequiredFields() throws Exception {
        // If any products exist in last 7 days via orders, check field shape
        var response = GET("/v1/reports/top-products?tenantId=" + TENANT + "&limit=10",
                bearer("admin1"))
                .andExpect(status().isOk())
                .andReturn();

        String body = response.getResponse().getContentAsString();
        if (!body.equals("[]")) {
            GET("/v1/reports/top-products?tenantId=" + TENANT + "&limit=10", bearer("admin1"))
                    .andExpect(jsonPath("$[0].productId").isNotEmpty())
                    .andExpect(jsonPath("$[0].productName").isNotEmpty())
                    .andExpect(jsonPath("$[0].totalQty").isNumber())
                    .andExpect(jsonPath("$[0].totalRevenue").isNumber())
                    .andExpect(jsonPath("$[0].orderCount").isNumber());
        }
    }

    @Test
    @DisplayName("FE-RPT-1 ✓ Cashier cannot access top products — returns 403")
    void cashier_topProducts_403() throws Exception {
        GET("/v1/reports/top-products?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-RPT-2 + FE-DASH-2: post-sale data consistency ────────────────────

    @Test
    @DisplayName("FE-DASH-2 ✓ Finance user can see revenue data (dashboard widget source)")
    void finance_canAccessRevenueSummaryAsDashboardWidget() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").isNumber())
                .andExpect(jsonPath("$.week").isNumber())
                .andExpect(jsonPath("$.month").isNumber());
    }
}
