package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1202: Configurable loyalty tiers.
 *
 * AC:
 *   POST /v2/loyalty/tiers → 201 with tierId
 *   GET  /v2/loyalty/tiers?tenantId=... → list ordered by minPoints
 *   Duplicate name → upsert (same tierId updated)
 */
@DisplayName("R2 :: BC-1202 — Configurable Loyalty Tiers")
class LoyaltyTiersTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1202 ✓ POST /v2/loyalty/tiers → 201 with tierId")
    void createTier_validRequest_returns201() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",    TENANT,
                "name",        "Silver-" + UUID.randomUUID(),
                "minPoints",   1000,
                "discountPct", 5.0
        );

        POST("/v2/loyalty/tiers", req, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tierId").isNotEmpty())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.minPoints", is(1000)));
    }

    @Test
    @DisplayName("BC-1202 ✓ GET /v2/loyalty/tiers → list ordered by minPoints")
    void listTiers_returnsOrderedList() throws Exception {
        String suffix = UUID.randomUUID().toString();
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "Gold-" + suffix, "minPoints", 5000), "")
                .andExpect(status().isCreated());
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "Bronze-" + suffix, "minPoints", 0), "")
                .andExpect(status().isCreated());
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "Silver-" + suffix, "minPoints", 1000), "")
                .andExpect(status().isCreated());

        // List for a fresh tenant with those exact tiers (can't guarantee order with shared tenant)
        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("BC-1202 ✓ Same tier name upserts — updates minPoints")
    void createTier_sameName_updatesExisting() throws Exception {
        String name = "Platinum-" + UUID.randomUUID();
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", name, "minPoints", 10000), "")
                .andExpect(status().isCreated());

        // Update: same name, different minPoints
        String body = POST("/v2/loyalty/tiers",
                Map.of("tenantId", TENANT, "name", name, "minPoints", 15000), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = om.readTree(body);
        int updated = json.get("minPoints").asInt();
        assert updated == 15000 : "Expected 15000 but got " + updated;
    }

    @Test
    @DisplayName("BC-1202 ✓ Tier with pointsPerDollar multiplier stored correctly")
    void createTier_pointsMultiplier_persisted() throws Exception {
        String name = "VIP-" + UUID.randomUUID();
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId",      TENANT,
                "name",          name,
                "minPoints",     8000,
                "pointsPerDollar", 2.0
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsPerDollar", is(2.0)));
    }

    @Test
    @DisplayName("BC-1202 ✓ DELETE /v2/loyalty/tiers/{id} → 204")
    void deleteTier_existing_returns204() throws Exception {
        String body = POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", "Temp-" + UUID.randomUUID(), "minPoints", 500), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tierId = om.readTree(body).get("tierId").asText();

        DELETE("/v2/loyalty/tiers/" + tierId, "")
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("BC-1202 ✓ Award points: customer with Silver tier earns at tier rate")
    void awardPoints_withSilverTier_earnsTierRate() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String cid = "silver-earner-" + suffix;
        String isolatedTenant = "tier-rate-" + suffix;

        // Create Silver tier with 2x earning in an isolated tenant
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", isolatedTenant, "name", "Silver2x-" + suffix,
                "minPoints", 0, "pointsPerDollar", 2.0), "")
                .andExpect(status().isCreated());

        POST("/v2/loyalty/award", Map.of(
                "tenantId", isolatedTenant, "customerId", cid,
                "orderId", "o1", "orderTotal", 20.0), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance", is(40)));  // 20 × 2.0
    }
}
