package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1201: Award loyalty points on completed purchase.
 *
 * AC:
 *   POST /v2/loyalty/award → 200 with updated balance
 *   Points = floor(orderTotal × rate)
 *   Balance increases on each award
 */
@DisplayName("R2 :: BC-1201 — Award Loyalty Points")
class LoyaltyAwardTest extends FunctionalTestBase {

    private static final String AWARD = "/v2/loyalty/award";
    private final String customerId = "loyalty-award-" + UUID.randomUUID();

    @Test
    @DisplayName("BC-1201 ✓ POST /v2/loyalty/award → 200 with pointsBalance")
    void award_validRequest_returns200WithBalance() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", customerId,
                "orderId",    "order-1",
                "orderTotal", 50.00
        );

        POST(AWARD, req, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").isNumber())
                .andExpect(jsonPath("$.pointsEarned", greaterThan(0)));
    }

    @Test
    @DisplayName("BC-1201 ✓ Points earned = floor(orderTotal × 1.0) by default")
    void award_defaultRate_earnsOnePointPerDollar() throws Exception {
        String cid = "ppt-calc-" + UUID.randomUUID();
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", cid,
                "orderId",    "order-calc",
                "orderTotal", 30.75
        );

        POST(AWARD, req, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(30)));  // floor(30.75)
    }

    @Test
    @DisplayName("BC-1201 ✓ Subsequent awards accumulate on balance")
    void award_twoPurchases_balanceAccumulates() throws Exception {
        String cid = "accum-" + UUID.randomUUID();
        Map<String, Object> req1 = Map.of("tenantId", TENANT, "customerId", cid,
                "orderId", "ord-a1", "orderTotal", 20.00);
        Map<String, Object> req2 = Map.of("tenantId", TENANT, "customerId", cid,
                "orderId", "ord-a2", "orderTotal", 30.00);

        POST(AWARD, req1, bearer("admin1")).andExpect(status().isOk());
        POST(AWARD, req2, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(50)));
    }

    @Test
    @DisplayName("BC-1201 ✓ New customer starts with Bronze tier")
    void award_newCustomer_bronzeTierInitialised() throws Exception {
        String cid = "bronze-" + UUID.randomUUID();
        Map<String, Object> req = Map.of("tenantId", TENANT, "customerId", cid,
                "orderId", "ord-b1", "orderTotal", 10.00);

        POST(AWARD, req, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierName", is("Bronze")));
    }

    @Test
    @DisplayName("BC-1201 ✓ Zero orderTotal → no points awarded, 200 returned")
    void award_zeroTotal_returnsOkWithZeroPoints() throws Exception {
        String cid = "zerototal-" + UUID.randomUUID();
        Map<String, Object> req = Map.of("tenantId", TENANT, "customerId", cid,
                "orderId", "ord-z1", "orderTotal", 0.00);

        POST(AWARD, req, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(0)));
    }

    @Test
    @DisplayName("BC-1201 ✓ Missing orderId → 400")
    void award_missingOrderId_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", customerId,
                "orderTotal", 10.00
        );

        POST(AWARD, req, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1201 ✓ GET /v2/loyalty/balance reflects earned points")
    void balance_afterAward_reflects_correct_balance() throws Exception {
        String cid = "bal-test-" + UUID.randomUUID();
        Map<String, Object> req = Map.of("tenantId", TENANT, "customerId", cid,
                "orderId", "ord-b", "orderTotal", 40.00);

        POST(AWARD, req, bearer("admin1")).andExpect(status().isOk());

        GET("/v2/loyalty/balance?tenantId=" + TENANT + "&customerId=" + cid, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(40)));
    }
}
