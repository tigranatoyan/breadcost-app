package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1401: Assign orders to delivery runs
 */
@DisplayName("R2 :: BC-1401 — Assign Orders to Delivery Runs")
class DeliveryRunAssignTest extends FunctionalTestBase {

    private String createRun(double courierCharge) throws Exception {
        String body = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverId", "drv-" + UUID.randomUUID().toString().substring(0, 8),
                "driverName", "John Smith",
                "scheduledDate", "2026-04-01",
                "courierCharge", courierCharge
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("runId").asText();
    }

    @Test
    @DisplayName("BC-1401 ✓ Create delivery run returns 201")
    void createRun_returns201() throws Exception {
        POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverId", "drv-001",
                "driverName", "Alice",
                "courierCharge", 25.00
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("BC-1401 ✓ Assign order to run returns 201")
    void assignOrder_returns201() throws Exception {
        String runId = createRun(20.0);
        String orderId = "ord-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT,
                "orderId", orderId
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("BC-1401 ✓ List run orders returns assigned orders")
    void listRunOrders_returnsAssigned() throws Exception {
        String runId = createRun(15.0);
        String orderId = "ord-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", orderId
        ), "").andExpect(status().isCreated());

        GET("/v2/delivery-runs/" + runId + "/orders", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId", hasItem(orderId)));
    }

    @Test
    @DisplayName("BC-1401 ✓ List delivery runs returns created run")
    void listRuns_returnsCreated() throws Exception {
        String runId = createRun(30.0);

        GET("/v2/delivery-runs?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].runId", hasItem(runId)));
    }

    @Test
    @DisplayName("BC-1401 ✓ Assign to non-existent run returns 400")
    void assignOrder_runNotFound_returns400() throws Exception {
        POST("/v2/delivery-runs/nonexistent/orders", Map.of(
                "tenantId", TENANT, "orderId", "ord-123"
        ), "")
                .andExpect(status().isBadRequest());
    }
}
