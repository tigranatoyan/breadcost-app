package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Delivery + PurchaseOrder API error paths and RBAC.
 * Both controllers are Admin/Manager only (class-level @PreAuthorize).
 */
@DisplayName("F2 :: Delivery & PurchaseOrder — Error Paths & RBAC")
class DeliveryPurchaseOrderApiErrorTest extends FunctionalTestBase {

    // ══════════════════════════════════════════════════════════════════════════
    // Delivery — class-level: Admin, Manager
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-DLV-1 ✓ Cashier cannot create delivery run (403)")
    void cashier_createDeliveryRun_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "date", "2025-07-01");
        POST("/v2/delivery-runs", body, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DLV-2 ✓ Warehouse cannot create delivery run (403)")
    void warehouse_createDeliveryRun_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "date", "2025-07-01");
        POST("/v2/delivery-runs", body, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DLV-3 ✓ Floor worker cannot list delivery runs (403)")
    void floorWorker_listDeliveryRuns_forbidden() throws Exception {
        GET("/v2/delivery-runs?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DLV-4 ✓ Technologist cannot get delivery run (403)")
    void technologist_getDeliveryRun_forbidden() throws Exception {
        GET("/v2/delivery-runs/any-id?tenantId=" + TENANT, bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DLV-5 ✓ Finance cannot assign order to run (403)")
    void finance_assignOrder_forbidden() throws Exception {
        POST("/v2/delivery-runs/run1/orders",
                Map.of("tenantId", TENANT, "orderId", "ord-1"), bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-DLV-6 ✓ Get nonexistent delivery run returns error")
    void getDeliveryRun_nonexistent_returnsError() throws Exception {
        GET("/v2/delivery-runs/does-not-exist?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Purchase Orders — class-level: Admin, Manager, Warehouse
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F2-PO-1 ✓ Cashier cannot list purchase orders (403)")
    void cashier_listPurchaseOrders_forbidden() throws Exception {
        GET("/v2/purchase-orders?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PO-2 ✓ Floor worker cannot list purchase orders (403)")
    void floorWorker_listPurchaseOrders_forbidden() throws Exception {
        GET("/v2/purchase-orders?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PO-3 ✓ Technologist cannot create purchase order (403)")
    void technologist_createPurchaseOrder_forbidden() throws Exception {
        POST("/v2/purchase-orders",
                Map.of("tenantId", TENANT, "supplierId", "s1"), bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PO-4 ✓ Finance cannot create purchase order (403)")
    void finance_createPurchaseOrder_forbidden() throws Exception {
        POST("/v2/purchase-orders",
                Map.of("tenantId", TENANT, "supplierId", "s1"), bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-PO-5 ✓ Get nonexistent purchase order returns error")
    void getPurchaseOrder_nonexistent_returnsError() throws Exception {
        GET("/v2/purchase-orders/does-not-exist?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }
}
