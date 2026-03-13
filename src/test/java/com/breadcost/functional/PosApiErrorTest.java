package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — POS API error paths, RBAC enforcement, and validation.
 * Complements existing PosFunctionalTest.
 */
@DisplayName("F2 :: POS API — Error Paths & RBAC")
class PosApiErrorTest extends FunctionalTestBase {

    // ── RBAC: write-only roles ────────────────────────────────────────────────

    @Test
    @DisplayName("F2-POS-1 ✓ Warehouse cannot create sale (403)")
    void warehouse_createSale_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "lines",
                List.of(Map.of("productId", "p1", "productName", "Bread",
                        "quantity", 1, "unitPrice", 5.0)),
                "paymentMethod", "CASH", "cashReceived", 10.0);
        POST("/v1/pos/sales", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-POS-2 ✓ Floor worker cannot create sale (403)")
    void floorWorker_createSale_forbidden() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "lines",
                List.of(Map.of("productId", "p1", "productName", "Bread",
                        "quantity", 1, "unitPrice", 5.0)),
                "paymentMethod", "CASH", "cashReceived", 10.0);
        POST("/v1/pos/sales", body, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-POS-3 ✓ Technologist cannot do stock-check (403)")
    void technologist_stockCheck_forbidden() throws Exception {
        POST("/v1/pos/stock-check?tenantId=" + TENANT,
                List.of(Map.of("productId", "p1", "productName", "Bread",
                        "quantity", 1, "unitPrice", 5.0)),
                bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-POS-4 ✓ Warehouse cannot reconcile (403)")
    void warehouse_reconcile_forbidden() throws Exception {
        POST("/v1/pos/reconcile", Map.of("tenantId", TENANT), bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    // ── Validation: missing @Valid fields ─────────────────────────────────────

    @Test
    @DisplayName("F2-POS-5 ✓ Create sale with blank tenantId returns 400")
    void createSale_blankTenantId_returns400() throws Exception {
        var body = Map.of(
                "tenantId", "",
                "lines", List.of(Map.of("productId", "p1", "productName", "Bread",
                        "quantity", 1, "unitPrice", 5.0)),
                "paymentMethod", "CASH");
        POST("/v1/pos/sales", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-POS-6 ✓ Create sale with empty lines returns 400")
    void createSale_emptyLines_returns400() throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "lines", List.of(),
                "paymentMethod", "CASH");
        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-POS-7 ✓ Create sale without paymentMethod returns 400")
    void createSale_noPaymentMethod_returns400() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "lines", List.of(Map.of("productId", "p1", "productName", "Bread",
                        "quantity", 1, "unitPrice", 5.0)));
        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isBadRequest());
    }
}
