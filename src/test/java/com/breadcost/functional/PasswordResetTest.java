package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2902: Customer password reset flow.
 *
 * AC:
 *   POST /v2/customers/forgot-password  → { resetToken }
 *   POST /v2/customers/reset-password   → 200 or 400/404
 *   Token expires after 1h.  Token single-use.
 */
@DisplayName("R4-S2 :: BC-2902 — Customer Password Reset")
class PasswordResetTest extends FunctionalTestBase {

    private static final String EMAIL = "reset-user@test.com";
    private static final String OLD_PASS = "OldPass123!";
    private static final String NEW_PASS = "NewPass456!";

    // ── Full flow ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2902 ✓ forgot → reset → login with new password")
    void fullResetFlow() throws Exception {
        // 1. Register customer
        POST("/v2/customers/register",
                Map.of("tenantId", TENANT, "name", "Reset User",
                        "email", EMAIL, "password", OLD_PASS), "")
                .andExpect(status().isCreated());

        // 2. Login with old password succeeds
        POST("/v2/customers/login",
                Map.of("tenantId", TENANT, "email", EMAIL, "password", OLD_PASS), "")
                .andExpect(status().isOk());

        // 3. Request password reset
        String body = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/v2/customers/forgot-password")
                                .contentType("application/json")
                                .content(json(Map.of("tenantId", TENANT, "email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String resetToken = om.readTree(body).get("resetToken").asText();

        // 4. Reset password
        POST("/v2/customers/reset-password",
                Map.of("token", resetToken, "newPassword", NEW_PASS), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password reset successful.")));

        // 5. Login with old password fails
        POST("/v2/customers/login",
                Map.of("tenantId", TENANT, "email", EMAIL, "password", OLD_PASS), "")
                .andExpect(status().isBadRequest());

        // 6. Login with new password succeeds
        POST("/v2/customers/login",
                Map.of("tenantId", TENANT, "email", EMAIL, "password", NEW_PASS), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    // ── Token reuse ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2902 ✓ used token rejected on second attempt")
    void usedToken_rejected() throws Exception {
        String email = "reuse-test@test.com";

        POST("/v2/customers/register",
                Map.of("tenantId", TENANT, "name", "Reuse User",
                        "email", email, "password", OLD_PASS), "")
                .andExpect(status().isCreated());

        // Request reset
        String body = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/v2/customers/forgot-password")
                                .contentType("application/json")
                                .content(json(Map.of("tenantId", TENANT, "email", email))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(body).get("resetToken").asText();

        // First use → success
        POST("/v2/customers/reset-password",
                Map.of("token", token, "newPassword", NEW_PASS), "")
                .andExpect(status().isOk());

        // Second use → 404 (token already used)
        POST("/v2/customers/reset-password",
                Map.of("token", token, "newPassword", "AnotherPass1!"), "")
                .andExpect(status().isNotFound());
    }

    // ── Invalid token ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2902 ✓ invalid token returns 404")
    void invalidToken_notFound() throws Exception {
        POST("/v2/customers/reset-password",
                Map.of("token", "non-existent-token", "newPassword", NEW_PASS), "")
                .andExpect(status().isNotFound());
    }

    // ── Non-existent email ────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2902 ✓ forgot-password for unknown email returns 404")
    void forgotPassword_unknownEmail() throws Exception {
        POST("/v2/customers/forgot-password",
                Map.of("tenantId", TENANT, "email", "nobody@nowhere.com"), "")
                .andExpect(status().isNotFound());
    }
}
