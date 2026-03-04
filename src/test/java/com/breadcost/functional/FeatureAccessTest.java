package com.breadcost.functional;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1702: Feature access by subscription tier.
 * Tests: tenant with STANDARD has REPORTS, BASIC missing REPORTS, ENTERPRISE has AI_BOT, no sub returns not allowed.
 */
public class FeatureAccessTest extends FunctionalTestBase {

    String adminToken() { return token("admin1"); }

    private void assignTier(String tenantId, String tier) throws Exception {
        PUT("/v2/subscriptions/tenants/" + tenantId, Map.of(
                "tierLevel", tier,
                "assignedBy", "superadmin"
        ), adminToken()).andExpect(status().isOk());
    }

    @Test
    void standardTenant_hasReportsFeature() throws Exception {
        String tenantId = "feature-std-" + System.currentTimeMillis();
        assignTier(tenantId, "STANDARD");

        GET("/v2/subscriptions/tenants/" + tenantId + "/features/REPORTS", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.featureKey").value("REPORTS"));
    }

    @Test
    void basicTenant_missingAiFeature() throws Exception {
        String tenantId = "feature-basic-" + System.currentTimeMillis();
        assignTier(tenantId, "BASIC");

        GET("/v2/subscriptions/tenants/" + tenantId + "/features/AI_BOT", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void enterpriseTenant_hasWhatsappFeature() throws Exception {
        String tenantId = "feature-ent-" + System.currentTimeMillis();
        assignTier(tenantId, "ENTERPRISE");

        GET("/v2/subscriptions/tenants/" + tenantId + "/features/WHATSAPP", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }
}
