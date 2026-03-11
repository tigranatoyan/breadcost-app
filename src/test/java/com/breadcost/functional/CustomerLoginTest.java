package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1102: Customer login and profile management
 *
 * AC:
 *   POST /v2/customers/login         → 200 { token, customerId, name }
 *   GET  /v2/customers/{id}/profile  → 200 customer object
 *   PUT  /v2/customers/{id}/profile  → 200 updated customer
 *   Invalid credentials              → 400
 */
@DisplayName("R2 :: BC-1102 — Customer Login & Profile")
class CustomerLoginTest extends FunctionalTestBase {

    private static final String REGISTER = "/v2/customers/register";
    private static final String LOGIN    = "/v2/customers/login";

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Register a customer and return their customerId */
    private String registerCustomer(String name, String email, String password) throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name",     name,
                "email",    email,
                "password", password
        );
        String body = POST(REGISTER, req, "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"customerId\":\"([^\"]+)\".*", "$1");
    }

    // ── Login success ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1102 ✓ POST /v2/customers/login → 200 with token, customerId, name")
    void login_validCredentials_returns200WithToken() throws Exception {
        registerCustomer("Lena Loginova", "lena@example.com", "pass123");

        Map<String, Object> req = Map.of(
                "tenantId",  TENANT,
                "email",     "lena@example.com",
                "password",  "pass123"
        );

        POST(LOGIN, req, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.customerId").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Lena Loginova")));
    }

    @Test
    @DisplayName("BC-1102 ✓ Login email case-insensitive")
    void login_emailCaseInsensitive_returns200() throws Exception {
        registerCustomer("Max Case", "max@example.com", "abc456");

        Map<String, Object> req = Map.of(
                "tenantId",  TENANT,
                "email",     "MAX@EXAMPLE.COM",
                "password",  "abc456"
        );

        POST(LOGIN, req, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ── Login failures ────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1102 ✓ Wrong password → 400")
    void login_wrongPassword_returns400() throws Exception {
        registerCustomer("Nina Wrong", "nina@example.com", "correct");

        Map<String, Object> req = Map.of(
                "tenantId",  TENANT,
                "email",     "nina@example.com",
                "password",  "wrong"
        );

        POST(LOGIN, req, "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1102 ✓ Non-existent email → 400")
    void login_unknownEmail_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",  TENANT,
                "email",     "ghost@example.com",
                "password",  "anything"
        );

        POST(LOGIN, req, "")
                .andExpect(status().isBadRequest());
    }

    // ── Profile GET ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1102 ✓ GET /v2/customers/{id}/profile → 200 with customer data")
    void getProfile_existingCustomer_returns200() throws Exception {
        String customerId = registerCustomer("Pavel Profile", "pavel@example.com", "prof99");

        GET("/v2/customers/" + customerId + "/profile?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.name", is("Pavel Profile")))
                .andExpect(jsonPath("$.email", is("pavel@example.com")));
    }

    @Test
    @DisplayName("BC-1102 ✓ GET profile with unknown customerId → 400")
    void getProfile_unknownId_returns400() throws Exception {
        GET("/v2/customers/no-such-id/profile?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ── Profile PUT ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1102 ✓ PUT /v2/customers/{id}/profile → 200 updated name/phone")
    void updateProfile_validRequest_returns200WithUpdatedFields() throws Exception {
        String customerId = registerCustomer("Old Name", "old.name@example.com", "pw1");

        Map<String, Object> update = Map.of(
                "name",  "New Name",
                "phone", "+37499123456"
        );

        PUT("/v2/customers/" + customerId + "/profile?tenantId=" + TENANT, update, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.phone", is("+37499123456")));
    }
}
