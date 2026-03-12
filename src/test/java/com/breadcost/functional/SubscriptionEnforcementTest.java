package com.breadcost.functional;

import com.breadcost.subscription.SubscriptionService;
import com.breadcost.subscription.SubscriptionTierRepository;
import com.breadcost.subscription.TenantSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-3101: Subscription enforcement via AOP.
 * BC-3102: maxUsers and maxProducts limits.
 */
class SubscriptionEnforcementTest extends FunctionalTestBase {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TenantSubscriptionRepository tenantSubRepo;

    @Autowired
    private SubscriptionTierRepository tierRepo;

    private void ensureTiers() {
        subscriptionService.seedTiers();
    }

    private void assignTier(String tier) {
        ensureTiers();
        // Deactivate any existing active subscription
        tenantSubRepo.findByTenantIdAndActive(TENANT, true).ifPresent(s -> {
            s.setActive(false);
            tenantSubRepo.save(s);
        });
        subscriptionService.assignTier(TENANT, tier, "test", LocalDate.now(), LocalDate.now().plusYears(1));
    }

    // ── BC-3101: Feature enforcement ─────────────────────────────────────────

    @Test
    void basicTier_aiEndpoint_returns403() throws Exception {
        assignTier("BASIC");
        // AI_BOT not in BASIC tier features
        POST(
            "/v3/ai/pricing/generate",
            Map.of("tenantId", TENANT),
            bearer("admin1")
        ).andExpect(status().isForbidden());
    }

    @Test
    void enterpriseTier_aiEndpoint_notBlocked() throws Exception {
        assignTier("ENTERPRISE");
        // AI_BOT is in ENTERPRISE tier — should not be 403
        int code = POST(
            "/v3/ai/pricing/generate",
            Map.of("tenantId", TENANT),
            bearer("admin1")
        ).andReturn().getResponse().getStatus();
        assert code != 403 : "Expected non-403 but got 403 — subscription incorrectly blocked";
    }

    @Test
    void basicTier_supplierEndpoint_returns403() throws Exception {
        assignTier("BASIC");
        // SUPPLIER not in BASIC tier
        GET(
            "/v2/suppliers?tenantId=" + TENANT,
            bearer("admin1")
        ).andExpect(status().isForbidden());
    }

    @Test
    void standardTier_loyaltyEndpoint_notBlocked() throws Exception {
        assignTier("STANDARD");
        // LOYALTY is in STANDARD tier
        int code = GET(
            "/v2/loyalty/tiers?tenantId=" + TENANT,
            bearer("admin1")
        ).andReturn().getResponse().getStatus();
        assert code != 403 : "Expected non-403 but got 403 — subscription incorrectly blocked";
    }

    // ── BC-3102: maxUsers limit ──────────────────────────────────────────────

    @Test
    void basicTier_userLimitEnforced() throws Exception {
        assignTier("BASIC"); // maxUsers=5
        // tenant1 already has 7 seed users from FunctionalTestBase
        // Creating another should fail with 409 (IllegalStateException → CONFLICT)
        POST(
            "/v1/users?tenantId=" + TENANT,
            Map.of(
                "tenantId", TENANT,
                "username", "limitTestUser",
                "password", "Test1234!",
                "displayName", "Limit Test",
                "roles", "Cashier"
            ),
            bearer("admin1")
        ).andExpect(status().isConflict())
         .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User limit reached")));
    }

    @Test
    void enterpriseTier_noUserLimit() throws Exception {
        assignTier("ENTERPRISE"); // maxUsers=0 (unlimited)
        // Should not be blocked by limit
        int code = POST(
            "/v1/users?tenantId=" + TENANT,
            Map.of(
                "tenantId", TENANT,
                "username", "unlimitedUser_" + System.nanoTime(),
                "password", "Test1234!",
                "displayName", "Unlimited Test",
                "roles", "Cashier"
            ),
            bearer("admin1")
        ).andReturn().getResponse().getStatus();
        assert code != 409 : "Expected non-409 but got 409 — user limit incorrectly enforced";
    }
}
