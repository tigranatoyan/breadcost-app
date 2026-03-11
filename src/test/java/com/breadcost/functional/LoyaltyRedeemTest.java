package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1204: Redeem loyalty points at checkout.
 */
@DisplayName("R2 :: BC-1204 — Redeem Loyalty Points")
class LoyaltyRedeemTest extends FunctionalTestBase {

    private String awardAndGetCustomer(int total) throws Exception {
        String cid = "redeem-" + UUID.randomUUID();
        POST("/v2/loyalty/award", Map.of(
                "tenantId", TENANT, "customerId", cid, "orderId", "seed-", "orderTotal", total
        ), bearer("admin1")).andExpect(status().isOk());
        return cid;
    }

    @Test
    @DisplayName("BC-1204 ✓ POST /v2/loyalty/redeem → 200 with reduced balance")
    void redeem_validRequest_returnsReducedBalance() throws Exception {
        String cid = awardAndGetCustomer(100);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 30, "orderId", "order-r1"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(70)));
    }

    @Test
    @DisplayName("BC-1204 ✓ Full balance redemption → balance becomes 0")
    void redeem_fullBalance_balanceZero() throws Exception {
        String cid = awardAndGetCustomer(50);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 50, "orderId", "order-r2"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(0)));
    }

    @Test
    @DisplayName("BC-1204 ✓ Redeem when insufficient balance → 400")
    void redeem_insufficientBalance_returns400() throws Exception {
        String cid = awardAndGetCustomer(10);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 9999, "orderId", "order-r3"
        ), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1204 ✓ pointsRedeemed accumulates correctly")
    void redeem_pointsRedeemedField_accumulates() throws Exception {
        String cid = awardAndGetCustomer(80);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 20, "orderId", "r-a"
        ), bearer("admin1")).andExpect(status().isOk());

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 10, "orderId", "r-b"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsRedeemed", is(30)));
    }

    @Test
    @DisplayName("BC-1204 ✓ Redeem with zero points → 400")
    void redeem_zeroPoints_returns400() throws Exception {
        String cid = awardAndGetCustomer(50);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 0, "orderId", "r-zero"
        ), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1204 ✓ Redeem transaction recorded in history")
    void redeem_transactionAppearsInHistory() throws Exception {
        String cid = awardAndGetCustomer(60);

        POST("/v2/loyalty/redeem", Map.of(
                "tenantId", TENANT, "customerId", cid,
                "points", 15, "orderId", "order-hist"
        ), bearer("admin1")).andExpect(status().isOk());

        GET("/v2/loyalty/history?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='REDEEM')]").isArray())
                .andExpect(jsonPath("$[?(@.type=='REDEEM')].points", hasItem(-15)));
    }
}
