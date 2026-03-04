package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1403: Mark delivery completed
 */
@DisplayName("R2 :: BC-1403 — Mark Delivery Completed")
class DeliveryCompleteTest extends FunctionalTestBase {

    private String[] createRunWithOrder() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT, "driverName", "Dave"
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId = "cmp-ord-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", orderId
        ), "").andExpect(status().isCreated());

        return new String[]{runId, orderId};
    }

    @Test
    @DisplayName("BC-1403 ✓ Mark order completed returns COMPLETED status")
    void markComplete_returnsCompleted() throws Exception {
        String[] pair = createRunWithOrder();

        PUT("/v2/delivery-runs/" + pair[0] + "/orders/" + pair[1] + "/complete?tenantId=" + TENANT,
                Map.of(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1403 ✓ Run auto-completes when all orders completed")
    void allOrdersComplete_runStatusBecomesCompleted() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT, "driverName", "Eve"
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId1 = "ord-" + UUID.randomUUID();
        String orderId2 = "ord-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", orderId1), "");
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", orderId2), "");

        PUT("/v2/delivery-runs/" + runId + "/orders/" + orderId1 + "/complete?tenantId=" + TENANT, Map.of(), "");
        PUT("/v2/delivery-runs/" + runId + "/orders/" + orderId2 + "/complete?tenantId=" + TENANT, Map.of(), "");

        GET("/v2/delivery-runs/" + runId + "?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("BC-1403 ✓ Mark non-existent order completed returns 400")
    void markComplete_notFound_returns400() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of("tenantId", TENANT), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();

        PUT("/v2/delivery-runs/" + runId + "/orders/nonexistent/complete?tenantId=" + TENANT,
                Map.of(), "")
                .andExpect(status().isBadRequest());
    }
}
