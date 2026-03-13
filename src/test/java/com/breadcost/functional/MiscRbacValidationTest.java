package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Misc controllers: Department, Supplier, Subscription, Loyalty, Config, Batch.
 * RBAC enforcement and validation for controllers with smaller surface area.
 */
@DisplayName("F2 :: Misc Controllers — RBAC & Validation")
class MiscRbacValidationTest extends FunctionalTestBase {

    // ═══════════════════════════════════════════════════════════════════════════
    // Departments — POST/PUT: Admin only
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-DEPT-1 ✓ Finance cannot create department (403)")
    void finance_createDepartment_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "name", "Pastry",
                "leadTimeHours", 4, "warehouseMode", "SHARED");
        POST("/v1/departments", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DEPT-2 ✓ Floor worker cannot create department (403)")
    void floorWorker_createDepartment_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "name", "Pastry",
                "leadTimeHours", 4, "warehouseMode", "SHARED");
        POST("/v1/departments", body, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DEPT-3 ✓ Create department with blank name returns 400")
    void createDepartment_blankName_returns400() throws Exception {
        var body = Map.of("tenantId", TENANT, "name", "",
                "leadTimeHours", 4, "warehouseMode", "SHARED");
        POST("/v1/departments", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Subscription — Admin only
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-SUB-1 ✓ Manager cannot list subscription tiers (403)")
    void manager_listSubscriptionTiers_forbidden() throws Exception {
        GET("/v2/subscriptions/tiers", bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-SUB-2 ✓ Finance cannot assign subscription tier (403)")
    void finance_assignTier_forbidden() throws Exception {
        PUT("/v2/subscriptions/tenants/" + TENANT,
                Map.of("tierLevel", "ENTERPRISE", "assignedBy", "admin1",
                        "startDate", "2025-01-01", "expiryDate", "2026-01-01"),
                bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-SUB-3 ✓ Cashier cannot deactivate expired subscriptions (403)")
    void cashier_deactivateExpired_forbidden() throws Exception {
        POST_noBody("/v2/subscriptions/deactivate-expired", bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Supplier — Admin, Manager, Warehouse + SUPPLIER subscription
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-SUP-1 ✓ Finance cannot list suppliers (403)")
    void finance_listSuppliers_forbidden() throws Exception {
        GET("/v2/suppliers?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-SUP-2 ✓ Cashier cannot create supplier (403)")
    void cashier_createSupplier_forbidden() throws Exception {
        POST("/v2/suppliers",
                Map.of("tenantId", TENANT, "name", "Test Supplier"), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-SUP-3 ✓ Create supplier with blank name returns 400")
    void createSupplier_blankName_returns400() throws Exception {
        POST("/v2/suppliers",
                Map.of("tenantId", TENANT, "name", ""), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Loyalty — various role restrictions
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-LOY-1 ✓ Warehouse cannot award loyalty points (403)")
    void warehouse_awardPoints_forbidden() throws Exception {
        POST("/v2/loyalty/award",
                Map.of("tenantId", TENANT, "customerId", "c1",
                        "orderId", "o1", "orderTotal", 100),
                bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-LOY-2 ✓ Cashier cannot award loyalty points (403)")
    void cashier_awardPoints_forbidden() throws Exception {
        POST("/v2/loyalty/award",
                Map.of("tenantId", TENANT, "customerId", "c1",
                        "orderId", "o1", "orderTotal", 100),
                bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-LOY-3 ✓ Technologist cannot list loyalty tiers (403)")
    void technologist_listLoyaltyTiers_forbidden() throws Exception {
        GET("/v2/loyalty/tiers?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Config — read: Admin/Finance/Viewer; write: Admin only
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-CFG-1 ✓ Warehouse cannot read config (403)")
    void warehouse_readConfig_forbidden() throws Exception {
        GET("/v1/config?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-CFG-2 ✓ Finance cannot update config (403)")
    void finance_updateConfig_forbidden() throws Exception {
        PUT("/v1/config?tenantId=" + TENANT,
                Map.of("displayName", "New Name"), bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Batch — issues: Admin, ProductionUser, ProductionSupervisor
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-BATCH-1 ✓ Cashier cannot issue to batch (403)")
    void cashier_issueToBatch_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "batchId", "b1",
                "itemId", "item1", "qty", 10, "uom", "kg",
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k1");
        POST("/v1/batches/b1/issues", body, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-BATCH-2 ✓ Finance cannot issue to batch (403)")
    void finance_issueToBatch_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "batchId", "b1",
                "itemId", "item1", "qty", 10, "uom", "kg",
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k2");
        POST("/v1/batches/b1/issues", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }
}
