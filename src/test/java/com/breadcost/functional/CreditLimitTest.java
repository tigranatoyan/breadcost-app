package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1504: Credit limit enforcement and overdue order block.
 * Tests: set credit limit, credit check allowed, credit check blocked, limit exceeded on invoice.
 */
public class CreditLimitTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    String createCustomer(String suffix) throws Exception {
        ResultActions ra = POST("/v2/customers/register", Map.of(
                "tenantId", TENANT,
                "name", "Credit Customer " + suffix,
                "email", "credit" + suffix + "@test.com",
                "password", "Test1234!"
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("customerId").asText();
    }

    @Test
    void setCreditLimit_savedOnCustomer() throws Exception {
        String customerId = createCustomer("cl1");
        PUT("/v2/customers/" + customerId + "/credit-limit",
                Map.of("tenantId", TENANT, "creditLimit", "500.00"),
                adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(500.0));
    }

    @Test
    void creditCheck_allowedWhenUnderLimit() throws Exception {
        String customerId = createCustomer("cl2");
        PUT("/v2/customers/" + customerId + "/credit-limit",
                Map.of("tenantId", TENANT, "creditLimit", "1000.00"),
                adminToken()).andExpect(status().isOk());

        GET("/v2/customers/" + customerId + "/credit-check?tenantId=" + TENANT + "&orderAmount=200.00",
                adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    void creditCheck_blockedWhenOverLimit() throws Exception {
        String customerId = createCustomer("cl3");
        PUT("/v2/customers/" + customerId + "/credit-limit",
                Map.of("tenantId", TENANT, "creditLimit", "50.00"),
                adminToken()).andExpect(status().isOk());

        // Generate invoice to consume some credit
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-cl3",
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "qty", "2", "unit", "loaf", "unitPrice", "20.00"))
        ), adminToken()).andExpect(status().isCreated());
        // outstanding = 48.00 (subtotal 40 + tax 8); check 20 more → 68 > 50 limit
        GET("/v2/customers/" + customerId + "/credit-check?tenantId=" + TENANT + "&orderAmount=20.00",
                adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void generateInvoice_failsWhenCreditExceeded() throws Exception {
        String customerId = createCustomer("cl4");
        PUT("/v2/customers/" + customerId + "/credit-limit",
                Map.of("tenantId", TENANT, "creditLimit", "10.00"),
                adminToken()).andExpect(status().isOk());

        // Invoice total would be 12 (10 subtotal + 2 tax) > 10 limit
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-cl4",
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "p1", "productName", "Big Loaf",
                                "qty", "1", "unit", "loaf", "unitPrice", "10.00"))
        ), adminToken()).andExpect(status().is4xxClientError());
    }
}
