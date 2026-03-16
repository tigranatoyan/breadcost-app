package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1101: Customer registration
 *
 * AC:
 *   POST /v2/customers/register with name, email, phone, delivery address(es)
 *   Returns 201 with customerId
 *   Duplicate email → 409
 *   FR-2.2 satisfied
 */
@DisplayName("R2 :: BC-1101 — Customer Registration")
class CustomerRegistrationTest extends FunctionalTestBase {

    private static final String URL = "/v2/customers/register";

    // ── Success path ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1101 ✓ POST /v2/customers/register → 201 + customerId")
    void register_success_returns201WithCustomerId() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Alice Bakery",
                "email", "alice@example.com",
                "phone", "+37499000001",
                "addresses", List.of(Map.of(
                        "label", "Main",
                        "line1", "123 Bread Street",
                        "city", "Yerevan",
                        "postalCode", "0001",
                        "countryCode", "AM"
                ))
        );

        POST(URL, req, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1101 ✓ Registration without optional phone/addresses → 201")
    void register_minimalPayload_returns201() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Bob Minimal",
                "email", "bob.minimal@example.com"
        );

        POST(URL, req, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1101 ✓ Multiple delivery addresses accepted")
    void register_multipleAddresses_returns201() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Carol Multi",
                "email", "carol.multi@example.com",
                "addresses", List.of(
                        Map.of("label", "Home", "line1", "1 Home St", "city", "Yerevan"),
                        Map.of("label", "Office", "line1", "5 Work Ave", "city", "Gyumri")
                )
        );

        POST(URL, req, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty());
    }

    // ── Duplicate email → 409 ────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1101 ✓ Duplicate email → 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Dave First",
                "email", "dave@example.com"
        );
        // First registration should succeed
        POST(URL, req, "").andExpect(status().isCreated());

        // Second registration with same email → 409
        Map<String, Object> dup = Map.of(
                "tenantId", TENANT,
                "name", "Dave Second",
                "email", "dave@example.com"
        );
        POST(URL, dup, "")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("BC-1101 ✓ Email case-insensitive duplicate detection")
    void register_duplicateEmailCaseInsensitive_returns409() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Eve Original",
                "email", "Eve@Example.com"
        );
        POST(URL, req, "").andExpect(status().isCreated());

        Map<String, Object> dup = Map.of(
                "tenantId", TENANT,
                "name", "Eve Dup",
                "email", "eve@example.com"   // same email, lowercase
        );
        POST(URL, dup, "")
                .andExpect(status().isConflict());
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1101 ✓ Missing name → 400")
    void register_missingName_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "email", "noname@example.com"
        );
        POST(URL, req, "").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1101 ✓ Invalid email format → 400")
    void register_invalidEmailFormat_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "name", "Bad Email",
                "email", "not-an-email"
        );
        POST(URL, req, "").andExpect(status().isBadRequest());
    }
}
