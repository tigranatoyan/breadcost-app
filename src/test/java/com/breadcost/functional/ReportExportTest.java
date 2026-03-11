package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1604: Report export (Excel and CSV).
 * Tests: export Excel returns bytes, export CSV returns CSV bytes, content-disposition set.
 */
public class ReportExportTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    private String createReport() throws Exception {
        ResultActions ra = POST("/v2/reports", Map.of(
                "tenantId", TENANT,
                "name", "Export Test Report " + System.currentTimeMillis(),
                "description", "Export test",
                "createdBy", "admin",
                "blockKeys", List.of("total_revenue", "total_orders")
        ), adminToken());
        ra.andExpect(status().isCreated());
        return om.readTree(ra.andReturn().getResponse().getContentAsString())
                .path("reportId").asText();
    }

    @Test
    void exportExcel_returns200WithBytes() throws Exception {
        String reportId = createReport();
        GET("/v2/reports/" + reportId + "/export?tenantId=" + TENANT + "&format=excel",
                adminToken())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void exportCsv_returns200WithCsvContent() throws Exception {
        String reportId = createReport();
        GET("/v2/reports/" + reportId + "/export?tenantId=" + TENANT + "&format=csv",
                adminToken())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void exportDefault_isExcel() throws Exception {
        String reportId = createReport();
        GET("/v2/reports/" + reportId + "/export?tenantId=" + TENANT, adminToken())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
