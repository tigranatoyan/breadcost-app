package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1503: Invoice payment status tracking.
 * Tests: mark paid, mark overdue, double payment rejected.
 */
class InvoicePaymentTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    String createCustomerAndInvoice(String suffix, String orderId) throws Exception {
        ResultActions ra = POST("/v2/customers/register", Map.of(
                "tenantId", TENANT,
                "name", "Pay Customer " + suffix,
                "email", "pay" + suffix + "@test.com",
                "password", "Test1234!"
        ), adminToken());
        ra.andExpect(status().isCreated());
        String customerId = om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("customerId").asText();

        ResultActions invRa = POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", orderId,
                "currency", "GBP",
                "lines", java.util.List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "qty", "1", "unit", "loaf", "unitPrice", "10.00"))
        ), adminToken());
        invRa.andExpect(status().isCreated());
        return om.readTree(invRa.andReturn().getResponse().getContentAsString())
                .path("invoice").path("invoiceId").asText();
    }

    @Test
    void markPaid_transitionsToPaymentRecorded() throws Exception {
        String invoiceId = createCustomerAndInvoice("ip1", "order-ip1");
        PUT("/v2/invoices/" + invoiceId + "/pay", Map.of(
                "tenantId", TENANT,
                "paidAmount", "12.00",
                "paidBy", "admin1"
        ), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidBy").value("admin1"));
    }

    @Test
    void markOverdue_transitionsIssuedToOverdue() throws Exception {
        String invoiceId = createCustomerAndInvoice("ip2", "order-ip2");
        // Issue it first
        PUT("/v2/invoices/" + invoiceId + "/issue?tenantId=" + TENANT, Map.of(), adminToken())
                .andExpect(status().isOk());

        PUT("/v2/invoices/" + invoiceId + "/overdue?tenantId=" + TENANT, Map.of(), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OVERDUE"));
    }

    @Test
    void markPaidTwice_rejected() throws Exception {
        String invoiceId = createCustomerAndInvoice("ip3", "order-ip3");
        Map<String, Object> body = Map.of(
                "tenantId", TENANT,
                "paidAmount", "12.00",
                "paidBy", "admin1"
        );
        PUT("/v2/invoices/" + invoiceId + "/pay", body, adminToken()).andExpect(status().isOk());
        PUT("/v2/invoices/" + invoiceId + "/pay", body, adminToken())
                .andExpect(status().is4xxClientError());
    }
}
