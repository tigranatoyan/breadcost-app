package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — Order API error paths, RBAC enforcement, and validation.
 * Complements existing OrdersFunctionalTest (happy paths) and
 * RoleAccessFunctionalTest (read-only matrix).
 */
@DisplayName("F2 :: Order API — Error Paths & RBAC")
class OrderApiErrorTest extends FunctionalTestBase {

    // ── RBAC: write endpoints ─────────────────────────────────────────────────

    @ParameterizedTest(name = "F2-ORD ✓ {0} cannot create order (403)")
    @CsvSource({"cashier1", "finance1", "manager1", "warehouse1"})
    void nonAdmin_createOrder_forbidden(String user) throws Exception {
        var body = Map.of(
                "tenantId", TENANT, "siteId", "site1",
                "customerName", "Test", "lines", List.of());
        POST("/v1/orders", body, bearer(user))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-ORD-5 ✓ Cashier cannot confirm order (403)")
    void cashier_confirmOrder_forbidden() throws Exception {
        POST_noBody("/v1/orders/fake-id/confirm?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-ORD-6 ✓ Finance cannot cancel order (403)")
    void finance_cancelOrder_forbidden() throws Exception {
        POST_noBody("/v1/orders/fake-id/cancel?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    // ── Not-found: confirm / cancel / status on nonexistent orders ────────────

    @Test
    @DisplayName("F2-ORD-7 ✓ Confirm nonexistent order returns 404")
    void confirm_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/orders/does-not-exist/confirm?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F2-ORD-8 ✓ Cancel nonexistent order returns 404")
    void cancel_nonexistent_returns404() throws Exception {
        POST_noBody("/v1/orders/does-not-exist/cancel?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── Bad request: invalid status value ─────────────────────────────────────

    @Test
    @DisplayName("F2-ORD-9 ✓ Advance to invalid status returns 400")
    void advanceStatus_invalidTarget_returns400() throws Exception {
        POST_noBody("/v1/orders/any-id/status?tenantId=" + TENANT + "&targetStatus=BOGUS",
                bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-ORD-10 ✓ Technologist cannot advance order status (403)")
    void technologist_advanceStatus_forbidden() throws Exception {
        POST_noBody("/v1/orders/any-id/status?tenantId=" + TENANT + "&targetStatus=CONFIRMED",
                bearer("tech1"))
                .andExpect(status().isForbidden());
    }
}
