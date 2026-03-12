package com.breadcost.functional;

import com.breadcost.masterdata.UserEntity;
import com.breadcost.masterdata.UserRepository;
import com.breadcost.security.JwtUtil;
import com.breadcost.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Shared base for all R1 functional (integration) tests.
 *
 * Uses the "test" Spring profile which switches to in-memory H2 so each test
 * run starts from a clean schema.  Helper methods:
 *
 *   token(username)            — returns a Bearer token for a pre-seeded user
 *   authHeader(token)          — wraps a token in the Authorization header value
 *   post/get/put/delete helpers — typed convenience wrappers over MockMvc
 *
 * Seed users (created in @BeforeEach):
 *   admin1   / Test1234!  — role Admin
 *   manager1 / Test1234!  — role Manager
 *   cashier1 / Test1234!  — role Cashier
 *   finance1 / Test1234!  — role FinanceUser
 *   warehouse1/ Test1234! — role Warehouse
 *   floor1   / Test1234!  — role ProductionUser
 *   tech1    / Test1234!  — role Technologist
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class FunctionalTestBase {

    static final String TENANT = "tenant1";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected SubscriptionService subscriptionService;

    // ── seed data ────────────────────────────────────────────────────────────

    @BeforeEach
    void seedUsers() {
        if (userRepository.findByUsername("admin1").isEmpty()) {
            save("admin1",     "Admin",          "Admin");
            save("manager1",   "Manager One",    "Manager");
            save("cashier1",   "Cashier One",    "Cashier");
            save("finance1",   "Finance One",    "FinanceUser");
            save("warehouse1", "Warehouse One",  "Warehouse");
            save("floor1",     "Floor One",      "ProductionUser");
            save("tech1",      "Technologist 1", "Technologist");
        }
        // Always ensure full feature access — tests that need a specific tier
        // override inside their own method
        subscriptionService.seedTiers();
        subscriptionService.assignTier(TENANT, "ENTERPRISE", "admin1",
                LocalDate.now(), LocalDate.now().plusYears(1));
    }

    private void save(String username, String display, String role) {
        UserEntity u = new UserEntity();
        u.setUserId(UUID.randomUUID().toString());
        u.setUsername(username);
        u.setDisplayName(display);
        u.setPasswordHash(passwordEncoder.encode("Test1234!"));
        u.setTenantId(TENANT);
        u.setRoles(role);
        u.setActive(true);
        userRepository.save(u);
    }

    // ── token helpers ─────────────────────────────────────────────────────────

    /** Generate a JWT for the named seed user. */
    protected String token(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Seed user not found: " + username));
        return jwtUtil.generate(user.getUsername(), user.getRoleList(), user.getTenantId());
    }

    protected String bearer(String username) {
        return "Bearer " + token(username);
    }

    // ── MockMvc helpers ───────────────────────────────────────────────────────

    protected ResultActions GET(String url, String bearerToken) throws Exception {
        return mvc.perform(get(url)
                .header("Authorization", bearerToken)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions POST(String url, Object body, String bearerToken) throws Exception {
        return mvc.perform(post(url)
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)));
    }

    protected ResultActions POST_noBody(String url, String bearerToken) throws Exception {
        return mvc.perform(post(url)
                .header("Authorization", bearerToken)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions PUT(String url, Object body, String bearerToken) throws Exception {
        return mvc.perform(put(url)
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)));
    }

    protected ResultActions DELETE(String url, String bearerToken) throws Exception {
        return mvc.perform(delete(url)
                .header("Authorization", bearerToken)
                .accept(MediaType.APPLICATION_JSON));
    }

    // ── JSON body builders ────────────────────────────────────────────────────

    protected String json(Object obj) throws Exception {
        return om.writeValueAsString(obj);
    }
}
