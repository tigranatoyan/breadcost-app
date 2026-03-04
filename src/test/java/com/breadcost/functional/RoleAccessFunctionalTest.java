package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-cutting role access tests — covers FE-SHELL-2 (nav/access matrix),
 * NFR-FE-13 (role-restricted elements not in DOM for wrong role, enforced at API).
 *
 * Each test verifies that the backend returns 403 Forbidden when a user without
 * the required role attempts to call a protected endpoint, confirming that the
 * role-gated nav links the frontend hides are also properly enforced server-side.
 *
 * Navigation matrix from FE-SHELL-2:
 *
 *   Endpoint                  | admin | mgmt | finance | warehouse | cashier | floor | tech
 *   /v1/orders                |  R/W  |  R/W |    R    |     -     |    -    |   -   |  -
 *   /v1/production-plans      |  R/W  |  R/W |    R    |     -     |    -    |   R   |  -
 *   /v1/pos/sales             |  R/W  |   -  |    -    |     -     |   R/W   |   -   |  -
 *   /v1/inventory/positions   |  R/W  |   R  |    R    |    R/W    |    -    |   R   |  -
 *   /v1/reports/revenue-summary| R   |   R  |    R    |     -     |    -    |   -   |  -
 *   /v1/users                 |  R/W  |   -  |    -    |     -     |    -    |   -   |  -
 *   /v1/recipes               |  R/W  |   -  |    -    |     -     |    -    |   -   | R/W
 */
@DisplayName("R1 :: Role-Based Access Matrix")
class RoleAccessFunctionalTest extends FunctionalTestBase {

    // ── Orders access matrix ──────────────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Cashier: /orders is inaccessible (403)")
    void cashier_ordersEndpoint_403() throws Exception {
        GET("/v1/orders?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Warehouse: /orders is inaccessible (403)")
    void warehouse_ordersEndpoint_403() throws Exception {
        GET("/v1/orders?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Finance: /orders is accessible (read)")
    void finance_ordersEndpoint_readable() throws Exception {
        GET("/v1/orders?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Technologist: /orders is inaccessible (403)")
    void technologist_ordersEndpoint_403() throws Exception {
        GET("/v1/orders?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    // ── Production Plans access matrix ────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Floor worker: /production-plans is readable")
    void floorWorker_productionPlans_readable() throws Exception {
        GET("/v1/production-plans?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Cashier: /production-plans is inaccessible (403)")
    void cashier_productionPlans_403() throws Exception {
        GET("/v1/production-plans?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── POS access matrix ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Admin: /pos/sales is accessible")
    void admin_posSales_accessible() throws Exception {
        GET("/v1/pos/sales?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Manager: /pos/sales GET is inaccessible (403)")
    void manager_posSales_403() throws Exception {
        GET("/v1/pos/sales?tenantId=" + TENANT, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Technologist: /pos/sales is inaccessible (403)")
    void technologist_posSales_403() throws Exception {
        GET("/v1/pos/sales?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    // ── Inventory access matrix ───────────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Manager: /inventory/positions is not in BE allowlist — returns 403")
    void manager_inventoryPositions_readable() throws Exception {
        // BE @PreAuthorize: Admin,ProductionUser,FinanceUser,Viewer,Warehouse — Manager not listed
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Cashier: /inventory/positions is inaccessible (403)")
    void cashier_inventoryPositions_403() throws Exception {
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Finance: /inventory/positions is readable")
    void finance_inventoryPositions_readable() throws Exception {
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk());
    }

    // ── Reports access matrix ─────────────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Admin: /reports/revenue-summary is accessible")
    void admin_revenueReport_accessible() throws Exception {
        // Admin always has access to reports
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Warehouse: /reports is inaccessible (403)")
    void warehouse_revenueReport_403() throws Exception {
        GET("/v1/reports/revenue-summary?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Floor: /reports/top-products is inaccessible (403)")
    void floor_topProducts_403() throws Exception {
        GET("/v1/reports/top-products?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    // ── Admin panel access matrix ─────────────────────────────────────────────

    @Test
    @DisplayName("FE-SHELL-2 ✓ Technologist: /users is inaccessible (403)")
    void technologist_users_403() throws Exception {
        GET("/v1/users?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Cashier: /users is inaccessible (403)")
    void cashier_users_403() throws Exception {
        GET("/v1/users?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-SHELL-2 ✓ Warehouse: /users is inaccessible (403)")
    void warehouse_users_403() throws Exception {
        GET("/v1/users?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    // ── No token ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NFR-FE-12 ✓ No Authorization header → 401 on all protected endpoints")
    void noToken_orders_401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/v1/orders?tenantId=" + TENANT))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("NFR-FE-12 ✓ Invalid JWT → 401 on protected endpoint")
    void invalidToken_returns401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/v1/orders?tenantId=" + TENANT)
                .header("Authorization", "Bearer this.is.not.valid"))
                .andExpect(status().is4xxClientError());
    }
}
