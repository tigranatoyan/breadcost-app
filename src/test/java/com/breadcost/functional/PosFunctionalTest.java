package com.breadcost.functional;

import com.breadcost.masterdata.SaleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for POS screen — covers FE-POS-1 through FE-POS-5.
 *
 * Requirements traced:
 *   FE-POS-1   Role access: Admin and Cashier can create sales
 *   FE-POS-4   POST /v1/pos/sales with CASH payment → change computed
 *   FE-POS-4   POST /v1/pos/sales with CARD payment → cardReference stored
 *   FE-POS-5   Sale response contains saleId for receipt screen
 *   FE-POS-1   Warehouse role cannot create sales (403)
 *   FE-POS-3   Cart lines reflected in response
 */
@DisplayName("R1 :: POS — Sales Endpoint")
class PosFunctionalTest extends FunctionalTestBase {

    @Autowired
    SaleRepository saleRepository;

    // ── FE-POS-4: CASH sale ───────────────────────────────────────────────────

    @Test
    @DisplayName("FE-POS-4 ✓ Cashier creates CASH sale — response has saleId + COMPLETED status")
    void cashier_createCashSale_success() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "siteId", "MAIN",
                "paymentMethod", "CASH",
                "cashReceived", 20000,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "White Bread",
                                "quantity", 2, "unit", "pcs", "unitPrice", 8000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.totalAmount").value(16000.0))
                .andExpect(jsonPath("$.changeGiven").value(4000.0));
    }

    @Test
    @DisplayName("FE-POS-4 ✓ Admin creates CARD sale — no changeGiven")
    void admin_createCardSale_success() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "siteId", "MAIN",
                "paymentMethod", "CARD",
                "cardReference", "TXN-123456",
                "lines", List.of(
                        Map.of("productId", "p2", "productName", "Sourdough",
                                "quantity", 1, "unit", "pcs", "unitPrice", 15000)
                )
        );

        POST("/v1/pos/sales", body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleId").isNotEmpty())
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.totalAmount").value(15000.0))
                .andExpect(jsonPath("$.changeGiven").value(0.0));
    }

    @Test
    @DisplayName("FE-POS-4 ✓ Multiple lines — totalAmount is sum of all lines")
    void createSale_multipleLines_totalIsCorrect() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "cashReceived", 50000,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bread A",
                                "quantity", 3, "unit", "pcs", "unitPrice", 5000),
                        Map.of("productId", "p2", "productName", "Bread B",
                                "quantity", 2, "unit", "pcs", "unitPrice", 8000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(31000.0))  // 3×5000 + 2×8000
                .andExpect(jsonPath("$.changeGiven").value(19000.0)); // 50000 - 31000
    }

    @Test
    @DisplayName("FE-POS-4 ✓ CASH payment with exact amount — changeGiven is 0")
    void createSale_cashExactAmount_changeIsZero() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "cashReceived", 12000,
                "lines", List.of(
                        Map.of("productId", "p3", "productName", "Pastry",
                                "quantity", 4, "unit", "pcs", "unitPrice", 3000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.changeGiven").value(0.0));
    }

    // ── FE-POS-5: cashierId recorded from JWT ─────────────────────────────────

    @Test
    @DisplayName("FE-POS-5 ✓ Sale records cashierId from JWT principal")
    void createSale_cashierIdFromJwt() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CARD",
                "lines", List.of(
                        Map.of("productId", "p9", "productName", "Bun",
                                "quantity", 1, "unit", "pcs", "unitPrice", 2000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashierId").value("cashier1"));
    }

    // ── FE-POS-1: role enforcement ────────────────────────────────────────────

    @Test
    @DisplayName("FE-POS-1 ✓ Warehouse role cannot create sales — returns 403")
    void warehouseRole_cannotCreateSale_403() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "cashReceived", 5000,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "quantity", 1, "unit", "pcs", "unitPrice", 5000)
                )
        );

        POST("/v1/pos/sales", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-POS-1 ✓ Finance role cannot create sales — returns 403")
    void financeRole_cannotCreateSale_403() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "cashReceived", 5000,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "quantity", 1, "unit", "pcs", "unitPrice", 5000)
                )
        );

        POST("/v1/pos/sales", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    // ── Invalid requests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-POS-4 ✓ Empty lines list returns 400")
    void createSale_emptyLines_returns400() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "lines", List.of()
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FE-POS-4 ✓ Missing paymentMethod returns 400")
    void createSale_missingPaymentMethod_returns400() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "quantity", 1, "unit", "pcs", "unitPrice", 5000)
                )
        );

        POST("/v1/pos/sales", body, bearer("cashier1"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /v1/pos/sales ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-POS-5 ✓ Admin can list sales")
    void admin_listSales_succeeds() throws Exception {
        GET("/v1/pos/sales?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-POS-1 ✓ Floor worker cannot list sales — returns 403")
    void floorRole_cannotListSales_403() throws Exception {
        GET("/v1/pos/sales?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }
}
