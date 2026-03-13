package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 — User management API validation and RBAC, plus extra auth edge cases.
 * Complements existing UserManagementFunctionalTest and AuthFunctionalTest.
 */
@DisplayName("F2 :: User & Auth — Validation & Error Paths")
class UserAuthApiErrorTest extends FunctionalTestBase {

    // ── User RBAC: more role combinations ─────────────────────────────────────

    @Test
    @DisplayName("F2-USR-1 ✓ Floor worker cannot list users (403)")
    void floorWorker_listUsers_forbidden() throws Exception {
        GET("/v1/users?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-USR-2 ✓ Finance cannot create user (403)")
    void finance_createUser_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "username", "newguy",
                "password", "Test1234!", "roles", "Cashier");
        POST("/v1/users", body, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2-USR-3 ✓ Manager cannot create user (403)")
    void manager_createUser_forbidden() throws Exception {
        var body = Map.of("tenantId", TENANT, "username", "newguy",
                "password", "Test1234!", "roles", "Cashier");
        POST("/v1/users", body, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    // ── User Validation: @Valid on CreateUserRequest ──────────────────────────

    @Test
    @DisplayName("F2-USR-4 ✓ Create user with blank username returns 400")
    void createUser_blankUsername_returns400() throws Exception {
        var body = Map.of("tenantId", TENANT, "username", "",
                "password", "Test1234!", "roles", "Cashier");
        POST("/v1/users", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-USR-5 ✓ Create user with blank password returns 400")
    void createUser_blankPassword_returns400() throws Exception {
        var body = Map.of("tenantId", TENANT, "username", "newguy",
                "password", "", "roles", "Cashier");
        POST("/v1/users", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-USR-6 ✓ Create user with blank tenantId returns 400")
    void createUser_blankTenantId_returns400() throws Exception {
        var body = Map.of("tenantId", "", "username", "newguy",
                "password", "Test1234!", "roles", "Cashier");
        POST("/v1/users", body, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ── User: reset-password validation ───────────────────────────────────────

    @Test
    @DisplayName("F2-USR-7 ✓ Reset password with blank newPassword returns 400")
    void resetPassword_blankNewPassword_returns400() throws Exception {
        // Get admin1 userId first
        var result = GET("/v1/users?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andReturn();
        var users = om.readTree(result.getResponse().getContentAsString());
        String userId = users.get(0).get("userId").asText();

        POST("/v1/users/" + userId + "/reset-password?tenantId=" + TENANT,
                Map.of("newPassword", ""), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    // ── Auth: edge cases beyond AuthFunctionalTest ────────────────────────────

    @Test
    @DisplayName("F2-AUTH-1 ✓ Login with empty body returns 400")
    void login_emptyBody_returns400() throws Exception {
        POST("/v1/auth/login", Map.of(), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F2-AUTH-2 ✓ Auth /me without token returns 401")
    void me_noToken_returns401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
