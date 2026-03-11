package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2604: Customer "me" endpoint.
 *
 * AC:
 *   GET /v2/customers/me → returns authenticated customer's profile
 *   Uses customerId from JWT, no path parameter needed.
 *   401 if no token.
 */
@DisplayName("R4-S1 :: BC-2604 — Customer /me Endpoint")
class CustomerMeEndpointTest extends FunctionalTestBase {

    private static final String REGISTER = "/v2/customers/register";
    private static final String LOGIN    = "/v2/customers/login";

    @Test
    @DisplayName("BC-2604 ✓ GET /v2/customers/me → 200 with correct profile")
    void me_authenticatedCustomer_returnsProfile() throws Exception {
        // Register
        String regBody = POST(REGISTER, Map.of(
                "tenantId", TENANT,
                "name",     "Me Test User",
                "email",    "me-test@example.com",
                "password", "pass123"
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerId = om.readTree(regBody).get("customerId").asText();

        // Login
        String loginBody = POST(LOGIN, Map.of(
                "tenantId", TENANT,
                "email",    "me-test@example.com",
                "password", "pass123"
        ), "").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode loginJson = om.readTree(loginBody);
        String customerBearer = "Bearer " + loginJson.get("token").asText();

        // GET /me
        GET("/v2/customers/me", customerBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.name", is("Me Test User")))
                .andExpect(jsonPath("$.email", is("me-test@example.com")));
    }

    @Test
    @DisplayName("BC-2604 ✓ GET /v2/customers/me without token → 401")
    void me_noToken_returns401() throws Exception {
        GET("/v2/customers/me", "")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BC-2604 ✓ Staff cannot access /me (requires Customer role)")
    void me_staffUser_returns403() throws Exception {
        GET("/v2/customers/me", bearer("admin1"))
                .andExpect(status().isForbidden());
    }
}
