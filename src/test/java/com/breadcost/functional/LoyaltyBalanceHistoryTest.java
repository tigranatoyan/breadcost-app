package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1205: Points balance and history view.
 */
@DisplayName("R2 :: BC-1205 — Points Balance & History")
class LoyaltyBalanceHistoryTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1205 ✓ GET /v2/loyalty/balance → 200 for new customer (0 balance)")
    void balance_newCustomer_returnsZeroBalance() throws Exception {
        String cid = "newbal-" + UUID.randomUUID();

        GET("/v2/loyalty/balance?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(0)))
                .andExpect(jsonPath("$.pointsEarned", is(0)));
    }

    @Test
    @DisplayName("BC-1205 ✓ Balance reflects all awards")
    void balance_afterMultipleAwards_totalCorrect() throws Exception {
        String cid = "multiaward-" + UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            POST("/v2/loyalty/award", Map.of(
                    "tenantId", TENANT, "customerId", cid,
                    "orderId", "o" + i, "orderTotal", 10.0
            ), bearer("admin1")).andExpect(status().isOk());
        }

        GET("/v2/loyalty/balance?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(30)))
                .andExpect(jsonPath("$.pointsEarned", is(30)));
    }

    @Test
    @DisplayName("BC-1205 ✓ GET /v2/loyalty/history → 200 list newest first")
    void history_afterActivity_returnsTransactions() throws Exception {
        String cid = "hist-" + UUID.randomUUID();

        POST("/v2/loyalty/award", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "orderId", "h1", "orderTotal", 25.0
        ), bearer("admin1")).andExpect(status().isOk());

        POST("/v2/loyalty/award", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "orderId", "h2", "orderTotal", 15.0
        ), bearer("admin1")).andExpect(status().isOk());

        GET("/v2/loyalty/history?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].type", hasItem("EARN")));
    }

    @Test
    @DisplayName("BC-1205 ✓ History empty for brand-new customer")
    void history_newCustomer_returnsEmpty() throws Exception {
        String cid = "nohist-" + UUID.randomUUID();

        GET("/v2/loyalty/history?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("BC-1205 ✓ Balance account includes customerId and tenantId")
    void balance_responseIncludesIdentityFields() throws Exception {
        String cid = "identity-" + UUID.randomUUID();

        GET("/v2/loyalty/balance?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(cid)))
                .andExpect(jsonPath("$.tenantId", is(TENANT)));
    }

    @Test
    @DisplayName("BC-1205 ✓ History transactions include orderId reference")
    void history_transactionsHaveOrderId() throws Exception {
        String cid = "orderref-" + UUID.randomUUID();
        String orderId = "order-ref-" + UUID.randomUUID();

        POST("/v2/loyalty/award", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "orderId", orderId, "orderTotal", 20.0
        ), bearer("admin1")).andExpect(status().isOk());

        GET("/v2/loyalty/history?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId", is(orderId)));
    }
}
