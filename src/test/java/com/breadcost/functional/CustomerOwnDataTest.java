package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2603: Customer own-data-only access enforcement.
 *
 * AC:
 *   - Customer A cannot access customer B's profile → 403
 *   - Customer A can access own profile → 200
 *   - Admin can access any customer's profile → 200 (bypass)
 */
@DisplayName("R4-S1 :: BC-2603 — Customer Own-Data Enforcement")
class CustomerOwnDataTest extends FunctionalTestBase {

    private static final String REGISTER = "/v2/customers/register";
    private static final String LOGIN    = "/v2/customers/login";

    /** Register a customer, login, return {customerId, bearer} */
    private Map<String, String> registerAndLogin(String name, String email, String password) throws Exception {
        String regBody = POST(REGISTER, Map.of(
                "tenantId", TENANT,
                "name",     name,
                "email",    email,
                "password", password
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerId = om.readTree(regBody).get("customerId").asText();

        String loginBody = POST(LOGIN, Map.of(
                "tenantId", TENANT,
                "email",    email,
                "password", password
        ), "").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode loginJson = om.readTree(loginBody);
        String token = loginJson.get("token").asText();

        return Map.of("customerId", customerId, "bearer", "Bearer " + token);
    }

    // ── Profile access ────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2603 ✓ Customer can access own profile")
    void customerAccessOwnProfile_returns200() throws Exception {
        Map<String, String> customerA = registerAndLogin("Alice A", "alice-own@example.com", "pass1");

        GET("/v2/customers/" + customerA.get("customerId") + "/profile?tenantId=" + TENANT,
                customerA.get("bearer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice A"));
    }

    @Test
    @DisplayName("BC-2603 ✓ Customer cannot access another customer's profile → 403")
    void customerAccessOtherProfile_returns403() throws Exception {
        Map<String, String> customerA = registerAndLogin("Alice B", "alice-b@example.com", "pass1");
        Map<String, String> customerB = registerAndLogin("Bob B", "bob-b@example.com", "pass2");

        GET("/v2/customers/" + customerB.get("customerId") + "/profile?tenantId=" + TENANT,
                customerA.get("bearer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BC-2603 ✓ Admin bypasses own-data check → can view any customer profile")
    void adminAccessAnyProfile_returns200() throws Exception {
        Map<String, String> customer = registerAndLogin("Carol C", "carol-c@example.com", "pass1");

        GET("/v2/customers/" + customer.get("customerId") + "/profile?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carol C"));
    }

    // ── Order access ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2603 ✓ Customer cannot access another customer's order history → 403")
    void customerAccessOtherOrders_returns403() throws Exception {
        Map<String, String> customerA = registerAndLogin("Alice D", "alice-d@example.com", "pass1");
        Map<String, String> customerB = registerAndLogin("Bob D", "bob-d@example.com", "pass2");

        GET("/v2/orders?tenantId=" + TENANT + "&customerId=" + customerB.get("customerId"),
                customerA.get("bearer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BC-2603 ✓ Customer can access own order history")
    void customerAccessOwnOrders_returns200() throws Exception {
        Map<String, String> customer = registerAndLogin("Eve E", "eve-e@example.com", "pass1");

        GET("/v2/orders?tenantId=" + TENANT + "&customerId=" + customer.get("customerId"),
                customer.get("bearer"))
                .andExpect(status().isOk());
    }
}
