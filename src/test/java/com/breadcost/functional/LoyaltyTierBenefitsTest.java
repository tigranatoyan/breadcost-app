package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1203: Tier benefits management — discountPct, pointsPerDollar, benefitsDescription.
 */
@DisplayName("R2 :: BC-1203 — Tier Benefits Management")
class LoyaltyTierBenefitsTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1203 ✓ Tier benefits include discountPct")
    void createTier_withDiscount_persistsDiscountPct() throws Exception {
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", "Discount-" + UUID.randomUUID(),
                "minPoints", 500, "discountPct", 10.0
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountPct", is(10.0)));
    }

    @Test
    @DisplayName("BC-1203 ✓ Tier benefits include benefitsDescription")
    void createTier_withDescription_persistsDescription() throws Exception {
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", "Desc-" + UUID.randomUUID(),
                "minPoints", 200, "benefitsDescription", "Free delivery + priority support"
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.benefitsDescription", is("Free delivery + priority support")));
    }

    @Test
    @DisplayName("BC-1203 ✓ Update tier benefits via upsert")
    void updateTier_changesDiscountPct() throws Exception {
        String name = "Updatable-" + UUID.randomUUID();
        POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", name,
                "minPoints", 1000, "discountPct", 5.0), "").andExpect(status().isCreated());

        String body = POST("/v2/loyalty/tiers", Map.of("tenantId", TENANT, "name", name,
                "minPoints", 1000, "discountPct", 15.0), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = om.readTree(body);
        double updated = json.get("discountPct").asDouble();
        assert updated == 15.0 : "Expected 15.0 but got " + updated;
    }

    @Test
    @DisplayName("BC-1203 ✓ Tier list returns all benefits fields")
    void listTiers_includesBenefitsFields() throws Exception {
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", "BenefitsFull-" + UUID.randomUUID(),
                "minPoints", 3000, "discountPct", 7.5,
                "pointsPerDollar", 1.5, "benefitsDescription", "VIP benefits"
        ), "");

        GET("/v2/loyalty/tiers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].discountPct").exists())
                .andExpect(jsonPath("$[*].pointsPerDollar").exists());
    }

    @Test
    @DisplayName("BC-1203 ✓ Tier promotion: customer upgrades tier when crossing threshold")
    void tierPromotion_customerReachesSilver() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String cid = "tier-promo-" + suffix;

        // Create Silver tier at 50 points
        POST("/v2/loyalty/tiers", Map.of(
                "tenantId", TENANT, "name", "Silver50-" + suffix, "minPoints", 50
        ), "").andExpect(status().isCreated());

        // Award 60 points (order = $60)
        POST("/v2/loyalty/award", Map.of(
                "tenantId", TENANT, "customerId", cid, "orderId", "o1", "orderTotal", 60.0
        ), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierName", is("Silver50-" + suffix)));
    }

    @Test
    @DisplayName("BC-1203 ✓ Missing tenantId → 400")
    void createTier_missingTenantId_returns400() throws Exception {
        POST("/v2/loyalty/tiers", Map.of("name", "NoTenant", "minPoints", 0), "")
                .andExpect(status().isBadRequest());
    }
}
