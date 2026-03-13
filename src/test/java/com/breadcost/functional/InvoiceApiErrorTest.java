package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Invoice API error paths, RBAC enforcement, and not-found.
 * Entire controller requires Admin/Manager/FinanceUser + INVOICING subscription.
 */
@DisplayName("F2 :: Invoice API — Error Paths & RBAC")
class InvoiceApiErrorTest extends FunctionalTestBase {

    // ── RBAC: class-level Admin/Manager/FinanceUser guard ─────────────────────

    @Test
    @DisplayName("F2-INV-R1 ✓ Cashier cannot list invoices (403)")
    void cashier_listInvoices_forbidden() throws Exception {
        GET("/v2/invoices?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-R2 ✓ Warehouse cannot list invoices (403)")
    void warehouse_listInvoices_forbidden() throws Exception {
        GET("/v2/invoices?tenantId=" + TENANT, bearer("warehouse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-R3 ✓ Floor worker cannot generate invoice (403)")
    void floorWorker_generateInvoice_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "orderId", "ord-1");
        POST("/v2/invoices", body, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-R4 ✓ Technologist cannot issue invoice (403)")
    void technologist_issueInvoice_forbidden() throws Exception {
        PUT("/v2/invoices/fake-id/issue?tenantId=" + TENANT, Map.of(), bearer("tech1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-INV-R5 ✓ Cashier cannot mark invoice paid (403)")
    void cashier_markPaid_forbidden() throws Exception {
        PUT("/v2/invoices/fake-id/pay", Map.of("tenantId", TENANT), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── Not-found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("F2-INV-R6 ✓ Get nonexistent invoice returns 404")
    void getInvoice_nonexistent_returns404() throws Exception {
        GET("/v2/invoices/does-not-exist?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-INV-R7 ✓ Issue nonexistent invoice returns 404")
    void issueInvoice_nonexistent_returns404() throws Exception {
        PUT("/v2/invoices/does-not-exist/issue?tenantId=" + TENANT, Map.of(), bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-INV-R8 ✓ Cashier cannot update credit limit (403)")
    void cashier_updateCreditLimit_forbidden() throws Exception {
        PUT("/v2/customers/c1/credit-limit",
                Map.of("tenantId", TENANT, "creditLimit", 5000), bearer("cashier1"))
                .andExpect(status().isForbidden());
    }
}
