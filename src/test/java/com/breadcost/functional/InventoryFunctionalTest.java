package com.breadcost.functional;

import com.breadcost.masterdata.ItemEntity;
import com.breadcost.masterdata.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Inventory screen — covers FE-INV-1 through FE-INV-5.
 *
 * Requirements traced:
 *   FE-INV-1   Role access: warehouse full, admin/management read
 *   FE-INV-2   GET /v1/inventory/positions — stock level view
 *   FE-INV-3   POST /v1/inventory/receipts  — receive lot
 *   FE-INV-4   POST /v1/inventory/adjust   — inventory adjustment
 *   FE-INV-5   POST /v1/inventory/transfers — inventory transfer
 *   FE-DASH-2  GET /v1/inventory/alerts    — stock alert count (Dashboard widget)
 */
@DisplayName("R1 :: Inventory — Positions / Receive / Adjust / Transfer")
class InventoryFunctionalTest extends FunctionalTestBase {

    @Autowired
    private ItemRepository itemRepository;

    private String itemId;

    @BeforeEach
    void seedItem() {
        var existing = itemRepository.findByTenantId(TENANT).stream()
                .filter(i -> "Flour (Test Item)".equals(i.getName()))
                .findFirst();
        if (existing.isPresent()) {
            itemId = existing.get().getItemId();
            return;
        }
        ItemEntity item = new ItemEntity();
        item.setItemId(UUID.randomUUID().toString());
        item.setTenantId(TENANT);
        item.setName("Flour (Test Item)");
        item.setType("INGREDIENT");
        item.setBaseUom("KG");
        item.setActive(true);
        itemRepository.save(item);
        itemId = item.getItemId();
    }

    // ── FE-INV-2: positions ───────────────────────────────────────────────────

    @Test
    @DisplayName("FE-INV-2 ✓ Admin can view stock positions")
    void admin_getPositions_returnsArray() throws Exception {
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FE-INV-2 ✓ Warehouse user can view stock positions")
    void warehouse_getPositions_succeeds() throws Exception {
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-INV-1 ✓ Cashier cannot view stock positions — returns 403")
    void cashier_getPositions_403() throws Exception {
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-INV-3: receive lot ─────────────────────────────────────────────────

    @Test
    @DisplayName("FE-INV-3 ✓ Warehouse receives a lot — returns success result")
    void warehouse_receiveLot_succeeds() throws Exception {
        var body = Map.of(
                "receiptId",       UUID.randomUUID().toString(),
                "tenantId",        TENANT,
                "siteId",          "MAIN",
                "itemId",          itemId,
                "lotId",           UUID.randomUUID().toString(),
                "qty",             50.0,
                "uom",             "KG",
                "unitCostBase",    1200.0,
                "occurredAtUtc",   Instant.now().toString(),
                "idempotencyKey",  UUID.randomUUID().toString()
        );

        POST("/v1/inventory/receipts", body, bearer("warehouse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("FE-INV-3 ✓ Stock position increases after receiving lot")
    void receiveLot_positionIncreasesAfterReceipt() throws Exception {
        var body = Map.of(
                "receiptId",      UUID.randomUUID().toString(),
                "tenantId",       TENANT,
                "siteId",         "MAIN",
                "itemId",         itemId,
                "lotId",          UUID.randomUUID().toString(),
                "qty",            100.0,
                "uom",            "KG",
                "unitCostBase",   1000.0,
                "occurredAtUtc",  Instant.now().toString(),
                "idempotencyKey", UUID.randomUUID().toString()
        );

        POST("/v1/inventory/receipts", body, bearer("warehouse1"))
                .andExpect(status().isOk());

        // Position should now contain the item
        GET("/v1/inventory/positions?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == '" + itemId + "')]").exists());
    }

    @Test
    @DisplayName("FE-INV-1 ✓ Cashier cannot receive stock — returns 403")
    void cashier_receiveLot_403() throws Exception {
        var body = Map.of(
                "receiptId",      UUID.randomUUID().toString(),
                "tenantId",       TENANT,
                "siteId",         "MAIN",
                "itemId",         itemId,
                "lotId",          UUID.randomUUID().toString(),
                "qty",            10.0,
                "uom",            "KG",
                "unitCostBase",   1000.0,
                "occurredAtUtc",  Instant.now().toString(),
                "idempotencyKey", UUID.randomUUID().toString()
        );

        POST("/v1/inventory/receipts", body, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-INV-4: adjustment ──────────────────────────────────────────────────

    @Test
    @DisplayName("FE-INV-4 ✓ Warehouse adjusts stock (positive) — returns success")
    void warehouse_adjustInventory_positive_succeeds() throws Exception {
        var body = Map.of(
                "tenantId",       TENANT,
                "siteId",         "MAIN",
                "itemId",         itemId,
                "adjustmentQty",  5.0,
                "unit",           "kg",
                "reasonCode",     "COUNT_CORRECTION",
                "notes",          "Cycle count correction"
        );

        POST("/v1/inventory/adjust", body, bearer("warehouse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Adjustment applied"))
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.adjustmentQty").value(5.0));
    }

    @Test
    @DisplayName("FE-INV-4 ✓ Warehouse adjusts stock (negative for waste) — returns success")
    void warehouse_adjustInventory_negative_waste() throws Exception {
        var body = Map.of(
                "tenantId",       TENANT,
                "siteId",         "MAIN",
                "itemId",         itemId,
                "adjustmentQty",  -2.5,
                "unit",           "kg",
                "reasonCode",     "WASTE",
                "notes",          "Damaged bag"
        );

        POST("/v1/inventory/adjust", body, bearer("warehouse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustmentQty").value(-2.5))
                .andExpect(jsonPath("$.reasonCode").value("WASTE"));
    }

    @Test
    @DisplayName("FE-INV-1 ✓ Finance user cannot adjust inventory — returns 403")
    void financeUser_adjustInventory_403() throws Exception {
        var body = Map.of(
                "tenantId",    TENANT,
                "itemId",      itemId,
                "adjustmentQty", 1.0,
                "reasonCode",  "COUNT_CORRECTION"
        );

        POST("/v1/inventory/adjust", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    // ── FE-INV-5: transfer ────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-INV-5 ✓ Warehouse transfers stock between locations — returns success")
    void warehouse_transferInventory_succeeds() throws Exception {
        var body = Map.of(
                "tenantId",        TENANT,
                "siteId",          "MAIN",
                "itemId",          itemId,
                "qty",             10.0,
                "fromLocationId",  "MAIN",
                "toLocationId",    "BAKERY-A",
                "occurredAtUtc",   Instant.now().toString(),
                "idempotencyKey",  UUID.randomUUID().toString()
        );

        POST("/v1/inventory/transfers", body, bearer("warehouse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── FE-DASH-2: alerts ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-DASH-2 ✓ Admin can get inventory alerts")
    void admin_getAlerts_returnsArray() throws Exception {
        GET("/v1/inventory/alerts?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FE-DASH-2 ✓ Warehouse user can get inventory alerts")
    void warehouse_getAlerts_succeeds() throws Exception {
        GET("/v1/inventory/alerts?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isOk());
    }
}
