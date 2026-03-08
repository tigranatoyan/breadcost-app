package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1505: Customer-specific pricing and discount rules.
 * Tests: add discount rule, list rules, effective price with discount, delete rule.
 */
public class CustomerPricingTest extends FunctionalTestBase {

    String adminToken() { return token("admin1"); }

    String createCustomer(String suffix) throws Exception {
        ResultActions ra = POST("/v2/customers/register", Map.of(
                "tenantId", TENANT,
                "name", "Pricing Customer " + suffix,
                "email", "pricing" + suffix + "@test.com",
                "password", "Test1234!"
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("customerId").asText();
    }

    @Test
    void addDiscountRule_created() throws Exception {
        String customerId = createCustomer("pr1");
        POST("/v2/customers/" + customerId + "/discount-rules", Map.of(
                "tenantId", TENANT,
                "itemType", "PRODUCT",
                "itemId", "prod-001",
                "discountPct", "15.0",
                "minQty", "5"
        ), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleId").exists())
                .andExpect(jsonPath("$.discountPct").value(15.0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void listDiscountRules_returnsActiveRules() throws Exception {
        String customerId = createCustomer("pr2");
        POST("/v2/customers/" + customerId + "/discount-rules", Map.of(
                "tenantId", TENANT,
                "itemType", "PRODUCT",
                "itemId", "prod-002",
                "discountPct", "10.0",
                "minQty", "1"
        ), adminToken())
                .andExpect(status().isCreated());
        GET("/v2/customers/" + customerId + "/discount-rules?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void effectivePrice_appliesDiscount() throws Exception {
        String customerId = createCustomer("pr3");
        // 20% discount for product prod-003, min qty 1
        POST("/v2/customers/" + customerId + "/discount-rules", Map.of(
                "tenantId", TENANT,
                "itemType", "PRODUCT",
                "itemId", "prod-003",
                "discountPct", "20.0",
                "minQty", "1"
        ), adminToken())
                .andExpect(status().isCreated());

        // base 10.00, 20% off = 8.00
        GET("/v2/customers/" + customerId + "/effective-price?tenantId=" + TENANT
                + "&itemId=prod-003&basePrice=10.00&qty=1", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectivePrice").value(8.0));
    }

    @Test
    void invoiceAppliesDiscountRule_toLineTotal() throws Exception {
        String customerId = createCustomer("pr4");
        // 50% discount for product prod-004
        POST("/v2/customers/" + customerId + "/discount-rules", Map.of(
                "tenantId", TENANT,
                "itemType", "PRODUCT",
                "itemId", "prod-004",
                "discountPct", "50.0",
                "minQty", "1"
        ), adminToken())
                .andExpect(status().isCreated());

        // Invoice: 1 unit @ 10.00 with 50% off = lineTotal 5.00
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-pr4",
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "prod-004", "productName", "Fancy Loaf",
                                "qty", "1", "unit", "loaf", "unitPrice", "10.00"))
        ), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.subtotal").value(5.0));
    }
}
