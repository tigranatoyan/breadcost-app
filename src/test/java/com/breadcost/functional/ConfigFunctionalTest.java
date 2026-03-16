package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Tenant Configuration — covers FR-11.5.
 *
 * Requirements traced:
 *   FR-11.5   GET /v1/config — read tenant configuration
 *   FR-11.5   PUT /v1/config — update tenant configuration (Admin only)
 *   FR-11.5   Config includes: displayName, orderCutoffTime, rushOrderPremiumPct, mainCurrency
 */
@DisplayName("R1 :: Config — Tenant Configuration")
class ConfigFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/config";

    // ── FR-11.5: Read config ──────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.5 ✓ Admin can read tenant configuration")
    void admin_getConfig_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }

    @Test
    @DisplayName("FR-11.5 ✓ Finance user can read tenant configuration")
    void finance_getConfig_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-11.5 ✓ Config for new tenant returns default values")
    void getConfig_newTenant_returnsDefaults() throws Exception {
        GET(BASE + "?tenantId=brand-new-tenant", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("brand-new-tenant"));
    }

    // ── FR-11.5: Update config ────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.5 ✓ Admin updates tenant display name")
    void admin_updateConfig_displayName() throws Exception {
        var body = Map.of(
                "displayName", "My Bakery"
        );

        PUT(BASE + "?tenantId=" + TENANT, body, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("My Bakery"));
    }

    @Test
    @DisplayName("FR-11.5 ✓ Admin updates order cutoff time")
    void admin_updateConfig_cutoffTime() throws Exception {
        var body = Map.of(
                "orderCutoffTime", "16:00"
        );

        PUT(BASE + "?tenantId=" + TENANT, body, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCutoffTime").value("16:00"));
    }

    @Test
    @DisplayName("FR-11.5 ✓ Admin updates rush order premium and currency")
    void admin_updateConfig_rushAndCurrency() throws Exception {
        var body = Map.of(
                "rushOrderPremiumPct", 25.0,
                "mainCurrency", "AMD"
        );

        PUT(BASE + "?tenantId=" + TENANT, body, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rushOrderPremiumPct").value(25.0))
                .andExpect(jsonPath("$.mainCurrency").value("AMD"));
    }

    // ── Role enforcement ──────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.5 ✓ Finance user cannot update config — 403")
    void finance_updateConfig_forbidden() throws Exception {
        var body = Map.of("displayName", "Hacked Name");

        PUT(BASE + "?tenantId=" + TENANT, body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-11.5 ✓ Manager cannot update config — 403")
    void manager_updateConfig_forbidden() throws Exception {
        var body = Map.of("displayName", "Hacked Name");

        PUT(BASE + "?tenantId=" + TENANT, body, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-11.5 ✓ Cashier cannot read config — 403")
    void cashier_getConfig_forbidden() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── Partial update ────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR-11.5 ✓ Partial update — only displayName changes, others preserved")
    void partialUpdate_preservesOtherFields() throws Exception {
        // First set currency
        PUT(BASE + "?tenantId=" + TENANT,
                Map.of("mainCurrency", "USD"), bearer("admin1"))
                .andExpect(status().isOk());

        // Then update only displayName
        PUT(BASE + "?tenantId=" + TENANT,
                Map.of("displayName", "Partial Test"), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Partial Test"))
                .andExpect(jsonPath("$.mainCurrency").value("USD"));
    }
}
