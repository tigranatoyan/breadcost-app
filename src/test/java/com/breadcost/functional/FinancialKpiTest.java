package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1603: Advanced financial KPI computation.
 * Tests: compute single KPI, run full report, revenue KPI exists, all standard KPIs compute.
 */
public class FinancialKpiTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    private String createReport() throws Exception {
        ResultActions ra = POST("/v2/reports", Map.of(
                "tenantId", TENANT,
                "name", "KPI Test Report " + System.currentTimeMillis(),
                "description", "Test",
                "createdBy", "admin",
                "blockKeys", List.of("total_revenue", "gross_margin_pct", "total_orders")
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("reportId").asText();
    }

    @Test
    void computeSingleKpi_returnsValueAndUnit() throws Exception {
        GET("/v2/reports/kpi?blockKey=total_revenue&tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockKey").value("total_revenue"))
                .andExpect(jsonPath("$.value").exists())
                .andExpect(jsonPath("$.unit").value("GBP"));
    }

    @Test
    void computeGrossMarginKpi_hasPercentUnit() throws Exception {
        GET("/v2/reports/kpi?blockKey=gross_margin_pct&tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit").value("%"))
                .andExpect(jsonPath("$.value").isNumber());
    }

    @Test
    void runReport_returnsAllBlockResults() throws Exception {
        String reportId = createReport();
        GET("/v2/reports/" + reportId + "/run?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].blockKey").exists())
                .andExpect(jsonPath("$[0].value").exists());
    }

    @Test
    void computeDeliveryKpi_returnsStub() throws Exception {
        GET("/v2/reports/kpi?blockKey=delivery_completion_rate&tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").isNumber());
    }
}
