package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1502: Payment terms per customer.
 * Tests: set payment terms, invoice due date computed from terms, default 30 days.
 */
public class PaymentTermsTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    String createCustomer(String suffix) throws Exception {
        ResultActions ra = POST("/v2/customers/register", Map.of(
                "tenantId", TENANT,
                "name", "Terms Customer " + suffix,
                "email", "terms" + suffix + "@test.com",
                "password", "Test1234!",
                "phone", "0700000002"
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("customerId").asText();
    }

    @Test
    void defaultPaymentTermsIs30Days() throws Exception {
        String customerId = createCustomer("pt1");
        // Generate invoice and verify dueDate is 30 days from issuedDate
        ResultActions ra = POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-pt1",
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "qty", "1", "unit", "loaf", "unitPrice", "2.00"))
        ), adminToken());
        ra.andExpect(status().isCreated());
        com.fasterxml.jackson.databind.JsonNode inv = om.readTree(
                ra.andReturn().getResponse().getContentAsString()).path("invoice");
        org.junit.jupiter.api.Assertions.assertEquals(30, inv.path("paymentTermsDays").asInt());
    }

    @Test
    void updatePaymentTerms_changeSavedOnCustomer() throws Exception {
        String customerId = createCustomer("pt2");
        PUT("/v2/customers/" + customerId + "/payment-terms", Map.of(
                "tenantId", TENANT,
                "paymentTermsDays", 60
        ), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTermsDays").value(60));
    }

    @Test
    void invoiceInheritsCustomerPaymentTerms() throws Exception {
        String customerId = createCustomer("pt3");
        // Set 14-day terms
        PUT("/v2/customers/" + customerId + "/payment-terms",
                Map.of("tenantId", TENANT, "paymentTermsDays", 14),
                adminToken()).andExpect(status().isOk());

        // Generate invoice — should have 14 days
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-pt3",
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "qty", "1", "unit", "loaf", "unitPrice", "5.00"))
        ), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.paymentTermsDays").value(14));
    }
}
