package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1602: Custom report builder.
 * Tests: create report, list reports, get report, delete report.
 */
class CustomReportBuilderTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    private String createReport(String nameSuffix) throws Exception {
        ResultActions ra = POST("/v2/reports", Map.of(
                "tenantId", TENANT,
                "name", "Monthly Report " + nameSuffix,
                "description", "Test report",
                "createdBy", "admin",
                "blockKeys", List.of("total_revenue", "gross_margin_pct")
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("reportId").asText();
    }

    @Test
    void createReport_returnsCreatedReportWithBlocks() throws Exception {
        POST("/v2/reports", Map.of(
                "tenantId", TENANT,
                "name", "Weekly Sales " + System.currentTimeMillis(),
                "description", "Weekly overview",
                "createdBy", "admin",
                "blockKeys", List.of("total_revenue", "total_orders")
        ), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.blocks.length()").value(2));
    }

    @Test
    void listReports_returnsTenantReports() throws Exception {
        createReport("list1");
        createReport("list2");
        GET("/v2/reports?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    void getReport_returnsCorrectReport() throws Exception {
        String reportId = createReport("get1");
        GET("/v2/reports/" + reportId + "?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.blocks").exists());
    }

    @Test
    void deleteReport_softDeletes() throws Exception {
        String reportId = createReport("del1");
        DELETE("/v2/reports/" + reportId + "?tenantId=" + TENANT, adminToken())
                .andExpect(status().isNoContent());
        // Report should no longer appear in list
        GET("/v2/reports?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportId == '" + reportId + "')]").doesNotExist());
    }
}
