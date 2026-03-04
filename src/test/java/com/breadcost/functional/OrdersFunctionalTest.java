package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for Orders screen — covers FE-ORD-1 through FE-ORD-5.
 *
 * Requirements traced:
 *   FE-ORD-1   Role access: admin/management can list; floor cannot
 *   FE-ORD-2   GET /v1/orders — list with status filter
 *   FE-ORD-3   POST /v1/orders — create DRAFT order
 *   FE-ORD-3   POST /v1/orders/{id}/confirm — confirm DRAFT
 *   FE-ORD-4   POST /v1/orders/{id}/cancel  — cancel DRAFT or CONFIRMED
 *   FE-ORD-5   Status transitions follow BE state machine
 */
@DisplayName("R1 :: Orders — CRUD & Status Transitions")
class OrdersFunctionalTest extends FunctionalTestBase {

    private static final String BASE = "/v1/orders";

    // ── FE-ORD-2: List orders ─────────────────────────────────────────────────

    @Test
    @DisplayName("FE-ORD-2 ✓ Admin can list all orders for a tenant")
    void admin_listOrders_returnsArray() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("FE-ORD-2 ✓ Status filter — only DRAFT orders returned")
    void listOrders_statusFilter_returnsDraftOnly() throws Exception {
        // Create one order
        createOrderReturningId("admin1");

        GET(BASE + "?tenantId=" + TENANT + "&status=DRAFT", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("DRAFT"))));
    }

    @Test
    @DisplayName("FE-ORD-1 ✓ Floor worker (ProductionUser) can read orders — BE allows ProductionUser")
    void floorWorker_listOrders_200() throws Exception {
        // BE @PreAuthorize includes ProductionUser; spec gap vs FE-ORD-1 nav restriction
        GET(BASE + "?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FE-ORD-1 ✓ Finance user can read orders list")
    void financeUser_listOrders_succeeds() throws Exception {
        GET(BASE + "?tenantId=" + TENANT, bearer("finance1"))
                .andExpect(status().isOk());
    }

    // ── FE-ORD-3: Create order ────────────────────────────────────────────────

    @Test
    @DisplayName("FE-ORD-3 ✓ Create DRAFT order — returns 201 with orderId and DRAFT status")
    void admin_createOrder_returnsDraft() throws Exception {
        var body = buildCreateOrderRequest();

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }

    @Test
    @DisplayName("FE-ORD-3 ✓ Order lines reflected in response")
    void createOrder_linesInResponse() throws Exception {
        var body = buildCreateOrderRequest();

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].productName").value("Test Bread"));
    }

    @Test
    @DisplayName("FE-ORD-3 ✓ Total amount auto-calculated from lines")
    void createOrder_totalAmountCalculated() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "customerName", "Test Customer",
                "requestedDeliveryTime", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                "forceRush", false,
                "lines", List.of(
                        Map.of("productId", "prod-1", "productName", "Bread",
                                "qty", 3, "uom", "pcs", "unitPrice", 10000)
                )
        );

        POST(BASE, body, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(30000.0));
    }

    // ── FE-ORD-3 / FE-ORD-5: Confirm ─────────────────────────────────────────

    @Test
    @DisplayName("FE-ORD-3 ✓ Confirm DRAFT order → status becomes CONFIRMED")
    void confirmOrder_draftBecomesConfirmed() throws Exception {
        String orderId = createOrderReturningId("admin1");

        POST(BASE + "/" + orderId + "/confirm?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("FE-ORD-5 ✓ Already CONFIRMED order cannot be confirmed again — returns 4xx")
    void confirmOrder_alreadyConfirmed_fails() throws Exception {
        String orderId = createOrderReturningId("admin1");

        // Confirm once
        POST(BASE + "/" + orderId + "/confirm?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk());

        // Confirm again — should fail
        POST(BASE + "/" + orderId + "/confirm?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }

    // ── FE-ORD-4: Cancel ──────────────────────────────────────────────────────

    @Test
    @DisplayName("FE-ORD-4 ✓ Cancel DRAFT order → status becomes CANCELLED")
    void cancelOrder_draftBecomesCancelled() throws Exception {
        String orderId = createOrderReturningId("admin1");

        POST(BASE + "/" + orderId + "/cancel?tenantId=" + TENANT + "&reason=test+cancel",
                null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("FE-ORD-4 ✓ Cancel CONFIRMED order is also allowed → CANCELLED")
    void cancelOrder_confirmedBecomesCancelled() throws Exception {
        String orderId = createOrderReturningId("admin1");

        // First confirm
        POST(BASE + "/" + orderId + "/confirm?tenantId=" + TENANT, null, bearer("admin1"))
                .andExpect(status().isOk());

        // Then cancel
        POST(BASE + "/" + orderId + "/cancel?tenantId=" + TENANT + "&reason=changed+mind",
                null, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ── FE-ORD-4: Get single order ────────────────────────────────────────────

    @Test
    @DisplayName("FE-ORD-4 ✓ Get single order by ID returns correct data")
    void getOrder_byId_returnsCorrectOrder() throws Exception {
        String orderId = createOrderReturningId("admin1");

        GET(BASE + "/" + orderId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));
    }

    @Test
    @DisplayName("FE-ORD-4 ✓ Get non-existent order returns 404")
    void getOrder_notFound_returns404() throws Exception {
        GET(BASE + "/no-such-order?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    String createOrderReturningId(String asUser) throws Exception {
        MvcResult result = POST(BASE, buildCreateOrderRequest(), bearer(asUser))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("orderId").asText();
    }

    private Map<String, Object> buildCreateOrderRequest() {
        return Map.of(
                "tenantId", TENANT,
                "customerName", "Test Customer",
                "requestedDeliveryTime", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                "forceRush", false,
                "lines", List.of(
                        Map.of("productId", "prod-1", "productName", "Test Bread",
                                "qty", 2, "uom", "pcs", "unitPrice", 8000)
                )
        );
    }
}
