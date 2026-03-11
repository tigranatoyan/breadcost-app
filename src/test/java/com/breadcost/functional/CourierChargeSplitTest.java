package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1405: Split delivery courier charge calculation
 */
@DisplayName("R2 :: BC-1405 — Split Delivery Courier Charge")
class CourierChargeSplitTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1405 ✓ Courier charge split equally across 2 orders")
    void courierCharge_splitTwoOrders() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverName", "Split-Driver",
                "courierCharge", 20.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String ord1 = "spl-1-" + UUID.randomUUID();
        String ord2 = "spl-2-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", ord1), bearer("admin1"));
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", ord2), bearer("admin1"));

        GET("/v2/delivery-runs/" + runId + "/orders", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierCharge").value(10.0))
                .andExpect(jsonPath("$[1].courierCharge").value(10.0));
    }

    @Test
    @DisplayName("BC-1405 ✓ Single order receives full courier charge")
    void courierCharge_singleOrderGetsAll() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT, "courierCharge", 15.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId = "single-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", orderId), bearer("admin1"));

        GET("/v2/delivery-runs/" + runId + "/orders", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierCharge").value(15.0));
    }

    @Test
    @DisplayName("BC-1405 ✓ No courier charge → all orders show 0")
    void courierCharge_zeroCharge_noSplit() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT, "courierCharge", 0.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", "zero-" + UUID.randomUUID()
        ), bearer("admin1"));

        GET("/v2/delivery-runs/" + runId + "/orders", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierCharge").value(0.0));
    }
}
