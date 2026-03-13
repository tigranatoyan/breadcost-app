package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Inventory API error paths, RBAC enforcement, and validation.
 * Complements existing InventoryFunctionalTest.
 */
@DisplayName("F2 :: Inventory API — Error Paths & RBAC")
class InventoryApiErrorTest extends FunctionalTestBase {

    // ── RBAC: write ops with wrong roles ──────────────────────────────────────

    @Test
    @DisplayName("F2-INV-1 ✓ Manager cannot receive lot (403)")
    void manager_receiveLot_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "receiptId", "r1",
                "itemId", "item1", "lotId", "lot1", "qty", 10,
                "uom", "kg", "unitCostBase", 5.0,
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k1");
        POST("/v1/inventory/receipts", body, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-2 ✓ Finance cannot receive lot (403)")
    void finance_receiveLot_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "receiptId", "r1",
                "itemId", "item1", "lotId", "lot1", "qty", 10,
                "uom", "kg", "unitCostBase", 5.0,
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k2");
        POST("/v1/inventory/receipts", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-3 ✓ Technologist cannot adjust inventory (403)")
    void technologist_adjustInventory_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "itemId", "item1",
                "adjustmentQty", 5, "reasonCode", "COUNT_CORRECTION");
        POST("/v1/inventory/adjust", body, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-4 ✓ Cashier cannot transfer inventory (403)")
    void cashier_transferInventory_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "itemId", "item1",
                "qty", 5, "fromLocationId", "loc1", "toLocationId", "loc2",
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k3");
        POST("/v1/inventory/transfers", body, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-5 ✓ Floor worker cannot auto-plan (403)")
    void floorWorker_autoPlan_forbidden() throws Exception {
        POST_noBody("/v1/inventory/auto-plan?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-6 ✓ Finance cannot see low-stock (403)")
    void finance_lowStock_forbidden() throws Exception {
        GET("/v1/inventory/low-stock?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-7 ✓ Technologist cannot see low-stock (403)")
    void technologist_lowStock_forbidden() throws Exception {
        GET("/v1/inventory/low-stock?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    // ── Validation: missing required @Valid fields on receipts ─────────────────

    @Test
    @DisplayName("F2-INV-8 ✓ Receive lot with blank tenantId returns 400")
    void receiveLot_blankTenantId_returns400() throws Exception {
        var body = Map.of(
                "tenantId", "", "siteId", "site1", "receiptId", "r1",
                "itemId", "item1", "lotId", "lot1", "qty", 10,
                "uom", "kg", "unitCostBase", 5.0,
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k4");
        POST("/v1/inventory/receipts", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-INV-9 ✓ Transfer with blank fromLocationId returns 400")
    void transfer_blankFromLocation_returns400() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1", "itemId", "item1",
                "qty", 5, "fromLocationId", "", "toLocationId", "loc2",
                "occurredAtUtc", "2025-01-01T00:00:00Z", "idempotencyKey", "k5");
        POST("/v1/inventory/transfers", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
