package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1501: Generate invoice from delivered order.
 * Tests: invoice creation, line items, duplicate prevention, calculated totals.
 */
public class GenerateInvoiceTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    String createCustomer(String suffix) throws Exception {
        ResultActions ra = POST("/v2/customers/register", Map.of(
                "tenantId", TENANT,
                "name", "Invoice Customer " + suffix,
                "email", "invoice" + suffix + "@test.com",
                "password", "Test1234!",
                "phone", "0700000001"
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("customerId").asText();
    }

    @Test
    void generateInvoice_returnsCreatedWithLines() throws Exception {
        String customerId = createCustomer("inv1");
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-001",
                "currency", "GBP",
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Sourdough",
                                "qty", "2", "unit", "loaf", "unitPrice", "3.50"),
                        Map.of("productId", "p2", "productName", "Rye",
                                "qty", "1", "unit", "loaf", "unitPrice", "4.00"))
        ), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.invoiceId").exists())
                .andExpect(jsonPath("$.invoice.status").value("DRAFT"))
                .andExpect(jsonPath("$.invoice.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void generateInvoice_totalIncludesTax() throws Exception {
        String customerId = createCustomer("inv2");
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-002",
                "currency", "GBP",
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "White",
                                "qty", "1", "unit", "loaf", "unitPrice", "10.00"))
        ), adminToken())
                // subtotal=10, tax=2, total=12
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.subtotal").value(10.0))
                .andExpect(jsonPath("$.invoice.taxAmount").value(2.0))
                .andExpect(jsonPath("$.invoice.totalAmount").value(12.0));
    }

    @Test
    void generateInvoice_duplicateOrderRejected() throws Exception {
        String customerId = createCustomer("inv3");
        Map<String, Object> body = Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-003",
                "currency", "GBP",
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bun",
                                "qty", "1", "unit", "each", "unitPrice", "1.50"))
        );
        POST("/v2/invoices", body, adminToken()).andExpect(status().isCreated());
        // Second time same orderId → 409 Conflict
        POST("/v2/invoices", body, adminToken()).andExpect(status().is4xxClientError());
    }

    @Test
    void listInvoices_returnsForTenant() throws Exception {
        String customerId = createCustomer("inv4");
        POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-004",
                "currency", "GBP",
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Roll",
                                "qty", "5", "unit", "each", "unitPrice", "0.80"))
        ), adminToken()).andExpect(status().isCreated());
        GET("/v2/invoices?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void issueInvoice_transitionsDraftToIssued() throws Exception {
        String customerId = createCustomer("inv5");
        ResultActions ra = POST("/v2/invoices", Map.of(
                "tenantId", TENANT,
                "customerId", customerId,
                "orderId", "order-005",
                "currency", "GBP",
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bagel",
                                "qty", "3", "unit", "each", "unitPrice", "1.20"))
        ), adminToken());
        ra.andExpect(status().isCreated());
        String invoiceId = om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("invoice").path("invoiceId").asText();

        PUT("/v2/invoices/" + invoiceId + "/issue?tenantId=" + TENANT, Map.of(), adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }
}
