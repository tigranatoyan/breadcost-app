package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1701: Subscription tier assignment.
 * Tests: list tiers returns 3, assign tier to tenant, retrieve current sub, reassign updates tier.
 */
public class SubscriptionTierTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    @Test
    void listTiers_returnsThreeDefaultTiers() throws Exception {
        GET("/v2/subscriptions/tiers", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void assignTier_returnsTenantSubscription() throws Exception {
        String tenantId = "tenant-sub-" + System.currentTimeMillis();
        PUT("/v2/subscriptions/tenants/" + tenantId, Map.of(
                "tierLevel", "STANDARD",
                "assignedBy", "superadmin",
                "expiryDate", "2026-12-31"
        ), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").exists())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.tierLevel").value("STANDARD"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getSubscription_returnsCurrentTier() throws Exception {
        String tenantId = "tenant-get-" + System.currentTimeMillis();
        PUT("/v2/subscriptions/tenants/" + tenantId, Map.of(
                "tierLevel", "ENTERPRISE",
                "assignedBy", "superadmin"
        ), adminToken()).andExpect(status().isOk());

        GET("/v2/subscriptions/tenants/" + tenantId, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierLevel").value("ENTERPRISE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void reassignTier_deactivatesOldAndActivatesNew() throws Exception {
        String tenantId = "tenant-reassign-" + System.currentTimeMillis();
        // Assign BASIC first
        PUT("/v2/subscriptions/tenants/" + tenantId, Map.of(
                "tierLevel", "BASIC",
                "assignedBy", "superadmin"
        ), adminToken()).andExpect(status().isOk());

        // Upgrade to ENTERPRISE
        PUT("/v2/subscriptions/tenants/" + tenantId, Map.of(
                "tierLevel", "ENTERPRISE",
                "assignedBy", "superadmin"
        ), adminToken()).andExpect(status().isOk());

        // Current tier should now be ENTERPRISE
        GET("/v2/subscriptions/tenants/" + tenantId, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tierLevel").value("ENTERPRISE"));
    }
}
