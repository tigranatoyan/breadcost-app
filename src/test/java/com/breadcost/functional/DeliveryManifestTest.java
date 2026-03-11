package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1402: Delivery manifest generation
 */
@DisplayName("R2 :: BC-1402 — Delivery Manifest Generation")
class DeliveryManifestTest extends FunctionalTestBase {

    private String createRunWithOrders() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverName", "Bob",
                "scheduledDate", "2026-04-02",
                "courierCharge", 20.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", "mfst-ord-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated());

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", "mfst-ord-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated());

        return runId;
    }

    @Test
    @DisplayName("BC-1402 ✓ Get manifest returns 200 with run and orders")
    void getManifest_returns200() throws Exception {
        String runId = createRunWithOrders();

        GET("/v2/delivery-runs/" + runId + "/manifest?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.runId").value(runId))
                .andExpect(jsonPath("$.orders").isArray());
    }

    @Test
    @DisplayName("BC-1402 ✓ Manifest includes all assigned orders")
    void getManifest_includesOrders() throws Exception {
        String runId = createRunWithOrders();

        GET("/v2/delivery-runs/" + runId + "/manifest?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(2));
    }

    @Test
    @DisplayName("BC-1402 ✓ Manifest includes driver info")
    void getManifest_includesDriverInfo() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverId", "drv-42",
                "driverName", "Carol Courier"
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();

        GET("/v2/delivery-runs/" + runId + "/manifest?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.driverName").value("Carol Courier"));
    }

    @Test
    @DisplayName("BC-1402 ✓ Manifest for non-existent run returns 400")
    void getManifest_notFound_returns400() throws Exception {
        GET("/v2/delivery-runs/bad-run/manifest?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
