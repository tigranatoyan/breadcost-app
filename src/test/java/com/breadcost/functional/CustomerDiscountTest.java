package com.breadcost.functional;

import com.breadcost.customers.CustomerDiscountRuleEntity;
import com.breadcost.customers.CustomerDiscountRuleRepository;
import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2904: Customer Discount Rules Controller
 *
 * GET    /v2/customers/{id}/discounts → 200 list
 * POST   /v2/customers/{id}/discounts → 201 create (admin)
 * DELETE /v2/customers/{id}/discounts/{ruleId} → 204 (admin)
 */
@DisplayName("R4-S3 :: BC-2904 — Customer Discount Rules")
class CustomerDiscountTest extends FunctionalTestBase {

    private static final String DISC_TENANT = "discount-tenant";

    @Autowired private CustomerDiscountRuleRepository discountRepo;
    @Autowired private CustomerRepository customerRepo;

    private String customerId;

    @BeforeEach
    void seedCustomer() {
        customerId = "disc-cust-" + UUID.randomUUID();
        customerRepo.save(CustomerEntity.builder()
                .customerId(customerId).tenantId(DISC_TENANT)
                .name("Discount Test Customer").email(customerId + "@test.com")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .active(true).build());
    }

    @Test
    @DisplayName("BC-2904 ✓ GET discounts empty list → 200 []")
    void getDiscounts_emptyList() throws Exception {
        GET("/v2/customers/" + customerId + "/discounts?tenantId=" + DISC_TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("BC-2904 ✓ POST create discount → 201 with ruleId")
    void createDiscount_returns201() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", DISC_TENANT,
                "itemType", "PRODUCT",
                "itemId", "some-product-id",
                "discountPct", 10,
                "notes", "VIP discount"
        );

        POST("/v2/customers/" + customerId + "/discounts", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleId").isNotEmpty())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.discountPct", is(10)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("BC-2904 ✓ GET discounts after create → 200 with 1 rule")
    void getDiscountsAfterCreate() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", DISC_TENANT,
                "itemType", "PRODUCT",
                "discountPct", 5
        );
        POST("/v2/customers/" + customerId + "/discounts", req, bearer("admin1"))
                .andExpect(status().isCreated());

        GET("/v2/customers/" + customerId + "/discounts?tenantId=" + DISC_TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].discountPct").value(closeTo(5.0, 0.01)));
    }

    @Test
    @DisplayName("BC-2904 ✓ DELETE discount → 204")
    void deleteDiscount_returns204() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", DISC_TENANT,
                "discountPct", 15
        );
        String body = POST("/v2/customers/" + customerId + "/discounts", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ruleId = om.readTree(body).get("ruleId").asText();

        DELETE("/v2/customers/" + customerId + "/discounts/" + ruleId + "?tenantId=" + DISC_TENANT,
                bearer("admin1"))
                .andExpect(status().isNoContent());

        // Verify it's gone
        GET("/v2/customers/" + customerId + "/discounts?tenantId=" + DISC_TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("BC-2904 ✓ DELETE nonexistent rule → 404")
    void deleteNonexistent_returns404() throws Exception {
        DELETE("/v2/customers/" + customerId + "/discounts/no-such-rule?tenantId=" + DISC_TENANT,
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }
}
