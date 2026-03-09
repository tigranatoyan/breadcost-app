package com.breadcost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-cutting E2E scenario tests covering secondary requirement gaps:
 *
 *   FR-10.1  Dashboard aggregation — orders-summary, production-summary
 *   FR-8.5   POS daily reconciliation
 *   FR-3.9   Production plan full lifecycle: DRAFT → GENERATED → APPROVED → IN_PROGRESS → COMPLETED
 *   FR-3.10  Work order start/complete
 */
@DisplayName("E2E :: Cross-Cutting Scenarios")
class ScenarioFunctionalTest extends FunctionalTestBase {

    // ═════════════════════════════════════════════════════════════════════════
    // FR-10.1: Dashboard aggregation
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FR-10.1 ✓ Orders summary returns today count, value, and status breakdown")
    void ordersSummary_returnsAggregation() throws Exception {
        GET("/v1/reports/orders-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCount").isNumber())
                .andExpect(jsonPath("$.todayValue").isNumber())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.byStatus").exists());
    }

    @Test
    @DisplayName("FR-10.1 ✓ Orders summary reflects a newly created order")
    void ordersSummary_reflectsNewOrder() throws Exception {
        // Create an order
        createOrder("admin1");

        GET("/v1/reports/orders-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FR-10.1 ✓ Production summary returns plan counts by status")
    void productionSummary_returnsPlanCounts() throws Exception {
        GET("/v1/reports/production-summary?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlans").isNumber())
                .andExpect(jsonPath("$.completed").isNumber())
                .andExpect(jsonPath("$.draft").isNumber())
                .andExpect(jsonPath("$.totalWorkOrders").isNumber())
                .andExpect(jsonPath("$.completionRate").isNumber());
    }

    @Test
    @DisplayName("FR-10.1 ✓ Production summary with date range filter")
    void productionSummary_dateRangeFilter() throws Exception {
        String from = LocalDate.now().minusDays(30).toString();
        String to = LocalDate.now().toString();

        GET("/v1/reports/production-summary?tenantId=" + TENANT
                        + "&dateFrom=" + from + "&dateTo=" + to, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlans").isNumber());
    }

    @Test
    @DisplayName("FR-10.1 ✓ Floor worker can access production summary")
    void floorWorker_productionSummary_succeeds() throws Exception {
        GET("/v1/reports/production-summary?tenantId=" + TENANT, bearer("floor1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-10.1 ✓ Cashier cannot access orders summary — 403")
    void cashier_ordersSummary_forbidden() throws Exception {
        GET("/v1/reports/orders-summary?tenantId=" + TENANT, bearer("cashier1"))
                .andExpect(status().isForbidden());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FR-8.5: POS daily reconciliation
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FR-8.5 ✓ POS reconciliation returns cash/card totals and net sales")
    void posReconcile_returnsTotals() throws Exception {
        // First create a sale so there's data
        createSale("cashier1");

        var body = Map.of(
                "tenantId", TENANT,
                "date", LocalDate.now().toString()
        );

        POST("/v1/pos/reconcile", body, bearer("cashier1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashTotal").isNumber())
                .andExpect(jsonPath("$.cardTotal").isNumber())
                .andExpect(jsonPath("$.refunds").isNumber())
                .andExpect(jsonPath("$.netSales").isNumber())
                .andExpect(jsonPath("$.totalTransactions").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("FR-8.5 ✓ Reconciliation on day with no sales returns zero totals")
    void posReconcile_emptyDay_returnsZeros() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "date", "2020-01-01"
        );

        POST("/v1/pos/reconcile", body, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.netSales").value(0));
    }

    @Test
    @DisplayName("FR-8.5 ✓ Finance user can access reconciliation")
    void finance_posReconcile_succeeds() throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "date", LocalDate.now().toString()
        );

        POST("/v1/pos/reconcile", body, bearer("finance1"))
                .andExpect(status().isOk());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FR-3.9 / FR-3.10: Production plan full lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FR-3.9 ✓ Full plan lifecycle: DRAFT → GENERATED → APPROVED → IN_PROGRESS → COMPLETED")
    void planFullLifecycle() throws Exception {
        // 1. Create DRAFT
        String planId = createPlanReturningId("admin1");

        // 2. Generate → GENERATED
        POST_noBody("/v1/production-plans/" + planId + "/generate?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GENERATED"));

        // 3. Approve → APPROVED
        POST_noBody("/v1/production-plans/" + planId + "/approve?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 4. Start → IN_PROGRESS
        POST_noBody("/v1/production-plans/" + planId + "/start?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 5. Complete → COMPLETED
        POST_noBody("/v1/production-plans/" + planId + "/complete?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("FR-3.9 ✓ Cannot start a DRAFT plan — must be APPROVED first")
    void cannotStartDraftPlan() throws Exception {
        String planId = createPlanReturningId("admin1");

        POST_noBody("/v1/production-plans/" + planId + "/start?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("FR-3.10 ✓ Plan schedule endpoint returns schedule data")
    void planSchedule_returnsData() throws Exception {
        String planId = createPlanReturningId("admin1");

        // Generate first so work orders exist
        POST_noBody("/v1/production-plans/" + planId + "/generate?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());

        GET("/v1/production-plans/" + planId + "/schedule?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-3.10 ✓ Material requirements for a plan")
    void planMaterialRequirements() throws Exception {
        String planId = createPlanReturningId("admin1");

        POST_noBody("/v1/production-plans/" + planId + "/generate?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());

        GET("/v1/production-plans/" + planId + "/material-requirements?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POS → Reports integration: sale appears in revenue
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("E2E ✓ List POS sales by date returns array")
    void listSalesByDate_returnsArray() throws Exception {
        createSale("cashier1");

        GET("/v1/pos/sales?tenantId=" + TENANT + "&date=" + LocalDate.now(),
                bearer("cashier1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void createOrder(String asUser) throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "customerName", "E2E Customer",
                "requestedDeliveryTime", Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                "forceRush", false,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Bread",
                                "qty", 2, "uom", "pcs", "unitPrice", 8000)
                )
        );

        POST("/v1/orders", body, bearer(asUser))
                .andExpect(status().isCreated());
    }

    private void createSale(String asUser) throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "paymentMethod", "CASH",
                "cashReceived", 20000,
                "lines", List.of(
                        Map.of("productId", "p1", "productName", "Test Bread",
                                "quantity", 2, "unit", "pcs", "unitPrice", 5000)
                )
        );

        POST("/v1/pos/sales", body, bearer(asUser))
                .andExpect(status().isCreated());
    }

    private String createPlanReturningId(String asUser) throws Exception {
        var body = Map.of(
                "tenantId", TENANT,
                "planDate", LocalDate.now().plusDays(1).toString(),
                "shift", "MORNING"
        );

        MvcResult result = POST("/v1/production-plans", body, bearer(asUser))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = om.readTree(result.getResponse().getContentAsString());
        return node.get("planId").asText();
    }
}
