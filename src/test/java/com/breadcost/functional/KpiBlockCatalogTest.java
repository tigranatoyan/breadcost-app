package com.breadcost.functional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1601: KPI block catalog.
 * Tests: list active blocks, filter by category, add custom block, duplicate key fails.
 */
public class KpiBlockCatalogTest extends FunctionalTestBase {

    String adminToken() { return bearer("admin1"); }

    @Test
    void listKpiBlocks_returnsSeededCatalog() throws Exception {
        GET("/v2/reports/kpi-blocks", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber())
                .andExpect(jsonPath("$[?(@.blockKey == 'total_revenue')]").exists())
                .andExpect(jsonPath("$[?(@.blockKey == 'gross_margin_pct')]").exists());
    }

    @Test
    void filterByCategory_returnsOnlyMatchingBlocks() throws Exception {
        GET("/v2/reports/kpi-blocks/category/FINANCIAL", adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber())
                .andExpect(jsonPath("$[0].category").value("FINANCIAL"));
    }

    @Test
    void addKpiBlock_createdSuccessfully() throws Exception {
        ResultActions ra = POST("/v2/reports/kpi-blocks", Map.of(
                "blockKey", "custom_kpi_" + System.currentTimeMillis(),
                "name", "Custom KPI Block",
                "description", "A custom test KPI",
                "category", "FINANCIAL",
                "queryType", "AGGREGATE",
                "unit", "GBP"
        ), adminToken());
        ra.andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockKey").exists())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void addKpiBlock_duplicateKey_fails() throws Exception {
        String key = "dup_key_" + System.currentTimeMillis();
        POST("/v2/reports/kpi-blocks", Map.of(
                "blockKey", key,
                "name", "Block A",
                "description", "First",
                "category", "FINANCIAL",
                "queryType", "AGGREGATE",
                "unit", "GBP"
        ), adminToken()).andExpect(status().isCreated());

        POST("/v2/reports/kpi-blocks", Map.of(
                "blockKey", key,
                "name", "Block B",
                "description", "Duplicate",
                "category", "FINANCIAL",
                "queryType", "AGGREGATE",
                "unit", "GBP"
        ), adminToken()).andExpect(status().is4xxClientError());
    }
}
