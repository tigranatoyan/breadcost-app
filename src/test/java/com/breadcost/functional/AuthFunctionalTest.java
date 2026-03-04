package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Authentication — covers FE-LOGIN-2, FE-LOGIN-3, FE-LOGIN-4
 * and NFR-FE-11 (JWT in response), NFR-FE-12 (401 on bad credentials).
 *
 * Requirements traced:
 *   FE-LOGIN-2  Username + password fields required
 *   FE-LOGIN-3  POST /v1/auth/login → JWT on success; error on failure
 *   NFR-FE-11   JWT token returned as "token" field in login response
 *   NFR-FE-12   401 Unauthorized on wrong credentials
 */
@DisplayName("R1 :: Auth — Login Endpoint")
class AuthFunctionalTest extends FunctionalTestBase {

    // ── FE-LOGIN-3 / NFR-FE-11 ───────────────────────────────────────────────

    @Test
    @DisplayName("FE-LOGIN-3 ✓ Successful login returns JWT + user metadata")
    void login_success_returnsTokenAndMetadata() throws Exception {
        POST("/v1/auth/login", Map.of("username", "admin1", "password", "Test1234!"), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin1"))
                .andExpect(jsonPath("$.tenantId").value(TENANT))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.primaryRole").value("Admin"));
    }

    @Test
    @DisplayName("FE-LOGIN-3 ✓ Cashier login returns cashier primaryRole")
    void login_cashier_roleInResponse() throws Exception {
        POST("/v1/auth/login", Map.of("username", "cashier1", "password", "Test1234!"), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRole").value("Cashier"));
    }

    @Test
    @DisplayName("FE-LOGIN-3 ✓ Finance user login returns FinanceUser primaryRole")
    void login_finance_roleInResponse() throws Exception {
        POST("/v1/auth/login", Map.of("username", "finance1", "password", "Test1234!"), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRole").value("FinanceUser"));
    }

    // ── FE-LOGIN-3 / NFR-FE-12 ───────────────────────────────────────────────

    @Test
    @DisplayName("NFR-FE-12 ✓ Wrong password returns 401 with error message")
    void login_wrongPassword_returns401() throws Exception {
        POST("/v1/auth/login", Map.of("username", "admin1", "password", "WrongPass!"), "")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("NFR-FE-12 ✓ Non-existent user returns 401")
    void login_unknownUser_returns401() throws Exception {
        POST("/v1/auth/login", Map.of("username", "ghost", "password", "Test1234!"), "")
                .andExpect(status().isUnauthorized());
    }

    // ── FE-LOGIN-2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-LOGIN-2 ✓ Missing username returns 400 validation error")
    void login_missingUsername_returns400() throws Exception {
        POST("/v1/auth/login", Map.of("password", "Test1234!"), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FE-LOGIN-2 ✓ Missing password returns 400 validation error")
    void login_missingPassword_returns400() throws Exception {
        POST("/v1/auth/login", Map.of("username", "admin1"), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FE-LOGIN-2 ✓ Empty username returns 400 validation error")
    void login_emptyUsername_returns400() throws Exception {
        POST("/v1/auth/login", Map.of("username", "", "password", "Test1234!"), "")
                .andExpect(status().isBadRequest());
    }

    // ── NFR-FE-11 — deactivated user ─────────────────────────────────────────

    @Test
    @DisplayName("NFR-FE-11 ✓ Deactivated account returns 401 with clear message")
    void login_deactivatedUser_returns401() throws Exception {
        // Deactivate manager1
        var user = userRepository.findByUsername("manager1").orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        POST("/v1/auth/login", Map.of("username", "manager1", "password", "Test1234!"), "")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is deactivated"));

        // Restore
        user.setActive(true);
        userRepository.save(user);
    }

    // ── Protected endpoint without token ─────────────────────────────────────

    @Test
    @DisplayName("NFR-FE-12 ✓ Protected endpoint without JWT returns 401 or 403")
    void protectedEndpoint_noToken_returns4xx() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/v1/orders?tenantId=" + TENANT))
                .andExpect(status().is4xxClientError());
    }
}
