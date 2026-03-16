package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Admin Panel — User Management (FE-ADMIN-1, FE-ADMIN-3).
 *
 * Requirements traced:
 *   FE-ADMIN-1  Admin-only access
 *   FE-ADMIN-3  GET /v1/users      — list users with masked passwords
 *   FE-ADMIN-3  POST /v1/users     — create user
 *   FE-ADMIN-3  PUT  /v1/users/{id} — update user (role change, deactivate)
 *   NFR-FE-14   Password hashes must not appear in API responses
 */
@DisplayName("R1 :: Admin Panel — User Management")
class UserManagementFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/users";

    // ── FE-ADMIN-1: access control ─────────────────────────────────────────────

    @Test
    @DisplayName("FE-ADMIN-1 ✓ Admin can list users")
    void admin_listUsers_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FE-ADMIN-1 ✓ Management user cannot list users — returns 403")
    void management_listUsers_403() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-ADMIN-1 ✓ Finance user cannot list users — returns 403")
    void finance_listUsers_403() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-ADMIN-1 ✓ Cashier cannot list users — returns 403")
    void cashier_listUsers_403() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ── NFR-FE-14: password masking ────────────────────────────────────────────

    @Test
    @DisplayName("NFR-FE-14 ✓ User list response masks password hash with [protected]")
    void listUsers_passwordHashIsMasked() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].passwordHash", everyItem(is("[protected]"))));
    }

    // ── FE-ADMIN-3: create user ────────────────────────────────────────────────

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Admin creates a new cashier user — returns 201")
    void admin_createUser_succeeds() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        var body = Map.of(
                "tenantId",    TENANT,
                "username",    "cashier_" + unique,
                "password",    "NewPass123!",
                "displayName", "New Cashier",
                "roles",       "Cashier"
        );

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cashier_" + unique))
                .andExpect(jsonPath("$.passwordHash").value("[protected]"))
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Created user can log in with their credentials")
    void createdUser_canLogin() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "newuser_" + unique;

        // Create user
        POST(BASE, Map.of(
                "tenantId",    TENANT,
                "username",    username,
                "password",    "MyPass456!",
                "displayName", "Created User",
                "roles",       "FinanceUser"
        ), bearer("admin1")).andExpect(status().isCreated());

        // Immediately login
        POST("/v1/auth/login", Map.of("username", username, "password", "MyPass456!"), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.primaryRole").value("FinanceUser"));
    }

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Non-admin cannot create users — returns 403")
    void manager_createUser_403() throws Exception {
        var body = Map.of(
                "tenantId",  TENANT,
                "username",  "shouldnotcreate",
                "password",  "Test123!",
                "roles",     "Cashier"
        );

        POST(BASE, body, bearer("manager1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Duplicate username returns 4xx error")
    void createUser_duplicateUsername_fails() throws Exception {
        var body = Map.of(
                "tenantId",  TENANT,
                "username",  "admin1",  // already exists
                "password",  "Test123!",
                "roles",     "Cashier"
        );

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }

    // ── FE-ADMIN-3: update user ────────────────────────────────────────────────

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Admin updates user roles")
    void admin_updateUserRoles_succeeds() throws Exception {
        // Create a user to update
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "updatable_" + unique;

        MvcResult created = POST(BASE, Map.of(
                "tenantId",    TENANT,
                "username",    username,
                "password",    "Test123!",
                "displayName", "Updatable User",
                "roles",       "Cashier"
        ), bearer("admin1")).andExpect(status().isCreated()).andReturn();

        String userId = om.readTree(created.getResponse().getContentAsString())
                .get("userId").asText();

        // Update role
        PUT(BASE + "/" + userId + "?tenantId=" + TENANT,
                Map.of("roles", "FinanceUser"),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(containsString("FinanceUser")));
    }

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Admin deactivates a user")
    void admin_deactivateUser_succeeds() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "deactivatable_" + unique;

        MvcResult created = POST(BASE, Map.of(
                "tenantId",    TENANT,
                "username",    username,
                "password",    "Test123!",
                "roles",       "Cashier"
        ), bearer("admin1")).andExpect(status().isCreated()).andReturn();

        String userId = om.readTree(created.getResponse().getContentAsString())
                .get("userId").asText();

        PUT(BASE + "/" + userId + "?tenantId=" + TENANT,
                Map.of("active", false),
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ── FE-ADMIN-3: get single user ────────────────────────────────────────────

    @Test
    @DisplayName("FE-ADMIN-3 ✓ Admin can get user by ID with masked password")
    void admin_getUserById_passwordMasked() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        MvcResult created = POST(BASE, Map.of(
                "tenantId",    TENANT,
                "username",    "getbyid_" + unique,
                "password",    "Test123!",
                "roles",       "Cashier"
        ), bearer("admin1")).andExpect(status().isCreated()).andReturn();

        String userId = om.readTree(created.getResponse().getContentAsString())
                .get("userId").asText();

        GET(BASE + "/" + userId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").value("[protected]"));
    }
}
