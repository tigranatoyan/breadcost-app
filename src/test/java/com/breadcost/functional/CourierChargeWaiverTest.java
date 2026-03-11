package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1406: Courier charge waiver with authorisation
 */
@DisplayName("R2 :: BC-1406 — Courier Charge Waiver")
class CourierChargeWaiverTest extends FunctionalTestBase {

    private String[] createRunWithOrder() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "driverName", "Waiver-Driver",
                "courierCharge", 30.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String orderId = "wvr-" + UUID.randomUUID();
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of(
                "tenantId", TENANT, "orderId", orderId
        ), bearer("admin1")).andExpect(status().isCreated());
        return new String[]{runId, orderId};
    }

    @Test
    @DisplayName("BC-1406 ✓ Waiver sets courier charge to 0 and records authorizer")
    void waiveCharge_setsZeroAndRecordsBy() throws Exception {
        String[] pair = createRunWithOrder();

        PUT("/v2/delivery-runs/" + pair[0] + "/orders/" + pair[1] + "/waive", Map.of(
                "tenantId", TENANT,
                "waivedBy", "manager@breadcost.com"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierChargeWaived").value(true))
                .andExpect(jsonPath("$.courierCharge").value(0))
                .andExpect(jsonPath("$.waivedBy").value("manager@breadcost.com"));
    }

    @Test
    @DisplayName("BC-1406 ✓ Missing waivedBy → 400")
    void waiveCharge_missingAuthorizer_returns400() throws Exception {
        String[] pair = createRunWithOrder();

        PUT("/v2/delivery-runs/" + pair[0] + "/orders/" + pair[1] + "/waive", Map.of(
                "tenantId", TENANT
        ), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1406 ✓ Waiver does not affect other orders in same run")
    void waiveCharge_otherOrdersUnaffected() throws Exception {
        String runBody = POST("/v2/delivery-runs", Map.of(
                "tenantId", TENANT,
                "courierCharge", 20.00
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = om.readTree(runBody).get("runId").asText();
        String ord1 = "wvr-a-" + UUID.randomUUID();
        String ord2 = "wvr-b-" + UUID.randomUUID();

        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", ord1), bearer("admin1"));
        POST("/v2/delivery-runs/" + runId + "/orders", Map.of("tenantId", TENANT, "orderId", ord2), bearer("admin1"));

        // Waive ord1
        PUT("/v2/delivery-runs/" + runId + "/orders/" + ord1 + "/waive", Map.of(
                "tenantId", TENANT, "waivedBy", "admin"
        ), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierChargeWaived").value(true));

        // Both orders still in the run
        GET("/v2/delivery-runs/" + runId + "/orders", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
