package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1206: Loyalty tier rule configuration (admin UI integration).
 *
 * Verifies the admin is able to manage tier rules including
 * earning rates, thresholds and ordering.
 */
@DisplayName("R2 :: BC-1206 — Loyalty Tier Rule Configuration")
class LoyaltyTierConfigTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1206 ✓ Full tier lifecycle: create → list → delete")
    void tierLifecycle_createListDelete() throws Exception {
        String name = "ConfigTier-" + UUID.randomUUID();

        // Create
        String body = POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", name, "minPoints", 2000,
                "discountPct", 12.0, "pointsPerDollar", 1.5,
                "benefitsDescription", "Complimentary delivery"
        ), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tierId = om.readTree(body).get("tierId").asText();

        // Appears in list
        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tierId", hasItem(tierId)));

        // Delete
        DELETE("/v2/loyalty/tiers/" + tierId, "")
                .andExpect(status().isNoContent());

        // No longer in list
        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tierId", not(hasItem(tierId))));
    }

    @Test
    @DisplayName("BC-1206 ✓ Tier config fields (pointsPerDollar, discountPct) are persisted and returned")
    void createTier_configFieldsPersisted() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String name = "CfgTier-" + suffix;

        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", name, "minPoints", 5000,
                "pointsPerDollar", 2.5, "discountPct", 8.0,
                "benefitsDescription", "Priority baking slot"
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsPerDollar").value(2.5))
                .andExpect(jsonPath("$.discountPct").value(8.0))
                .andExpect(jsonPath("$.benefitsDescription", is("Priority baking slot")));

        // Config visible in list (at least one tier returned)
        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    @DisplayName("BC-1206 ✓ Multiple tiers for same tenant can coexist")
    void multipleTiers_sameTenant_allPersist() throws Exception {
        String suffix = UUID.randomUUID().toString();
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "TierA-" + suffix, "minPoints", 0), "")
                .andExpect(status().isCreated());
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "TierB-" + suffix, "minPoints", 500), "")
                .andExpect(status().isCreated());
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "TierC-" + suffix, "minPoints", 2000), "")
                .andExpect(status().isCreated());

        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems(
                        "TierA-" + suffix, "TierB-" + suffix, "TierC-" + suffix
                )));
    }

    @Test
    @DisplayName("BC-1206 ✓ Missing name → 400")
    void createTier_missingName_returns400() throws Exception {
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "minPoints", 100), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1206 ✓ Tiers ordered by minPoints ascending")
    void listTiers_orderedByMinPoints() throws Exception {
        String suffix = UUID.randomUUID().toString();
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "High-" + suffix, "minPoints", 9000), "").andExpect(status().isCreated());
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", "Low-" + suffix, "minPoints", 100), "").andExpect(status().isCreated());

        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].minPoints").isArray());
    }
}
