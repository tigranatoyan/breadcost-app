package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1404: Failed delivery recording and re-delivery workflow
 */
@DisplayName("R2 :: BC-1404 — Failed Delivery and Re-Delivery")
class DeliveryFailRedeliverTest extends FunctionalTestBase {

    private String createRunWithOrder(String suffix) throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT, "driverName", "FailDriver-" + suffix
        ), "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId = "fail-ord-" + suffix;
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", orderId
        ), "").andExpect(status().isCreated());
        return runId;
    }

    @Test
    @DisplayName("BC-1404 ✓ Mark order failed records failure reason")
    void markFailed_recordsReason() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String runBody = POST("/v2/delivery-runs", Map.of("tenantId", TENANT, "driverName", "F-" + suffix), "")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId = "f-ord-" + suffix;
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", orderId), "");

        PUT("/v2/delivery-runs/" + runId + "/orders/" + orderId + "/fail", Map.of(
                "tenantId", TENANT,
                "failureReason", "Customer not home"
        ), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Customer not home"));
    }

    @Test
    @DisplayName("BC-1404 ✓ Re-delivery assigns to new run")
    void redeliver_assignsToNewRun() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String run1Body = POST("/v2/delivery-runs", Map.of("tenantId", TENANT, "driverName", "R1-" + suffix), "")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String run1Id = om.readTree(run1Body).get("runId").asText();
        String orderId = "r-ord-" + suffix;
        POST("/v2/delivery-runs/" + run1Id + "/orders", Map.of("tenantId", TENANT, "orderId", orderId), "");

        // Fail first
        PUT("/v2/delivery-runs/" + run1Id + "/orders/" + orderId + "/fail",
                Map.of("tenantId", TENANT, "failureReason", "Road closed"), "");

        // Create new run
        String run2Body = POST("/v2/delivery-runs", Map.of("tenantId", TENANT, "driverName", "R2-" + suffix), "")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String run2Id = om.readTree(run2Body).get("runId").asText();

        // Re-deliver
        POST("/v2/delivery-runs/" + run1Id + "/orders/" + orderId + "/redeliver", Map.of(
                "tenantId", TENANT,
                "newRunId", run2Id
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(run2Id))
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    @DisplayName("BC-1404 ✓ Fail non-existent order in run returns 400")
    void markFailed_notFound_returns400() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of("tenantId", TENANT), "")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();

        PUT("/v2/delivery-runs/" + runId + "/orders/nosuchorder/fail",
                Map.of("tenantId", TENANT, "failureReason", "oops"), "")
                .andExpect(status().isBadRequest());
    }
}
