package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2202: Supplier API integration tests (FR-6.4)
 */
@DisplayName("R3 :: BC-2202 — Supplier API Config")
class SupplierApiConfigTest extends FunctionalTestBase {

    private static final String BASE = "/v3/supplier-api";

    private String createSupplier() throws Exception {
        String body = POST("/v2/suppliers", Map.of(
                "tenantId", TENANT,
                "name", "ApiSupplier-" + UUID.randomUUID()
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("supplierId").asText();
    }

    @Test
    @DisplayName("BC-2202 ✓ Save API config returns 201")
    void saveConfig_returns201() throws Exception {
        String supplierId = createSupplier();

        POST(BASE + "/configs", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "apiUrl", "https://api.supplier.example/orders",
                "apiKeyRef", "vault:supplier-key-1",
                "format", "JSON",
                "enabled", true
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configId").isNotEmpty())
                .andExpect(jsonPath("$.apiUrl").value("https://api.supplier.example/orders"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("BC-2202 ✓ List configs returns saved config")
    void listConfigs_returnsSaved() throws Exception {
        String supplierId = createSupplier();

        POST(BASE + "/configs", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "apiUrl", "https://api.test.example/v1",
                "enabled", false
        ), bearer("admin1")).andExpect(status().isCreated());

        GET(BASE + "/configs?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].apiUrl", hasItem("https://api.test.example/v1")));
    }

    @Test
    @DisplayName("BC-2202 ✓ Get config by supplier ID")
    void getConfig_bySupplier() throws Exception {
        String supplierId = createSupplier();

        POST(BASE + "/configs", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "apiUrl", "https://api.single.example",
                "enabled", true
        ), bearer("admin1")).andExpect(status().isCreated());

        GET(BASE + "/configs/" + supplierId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(supplierId));
    }

    @Test
    @DisplayName("BC-2202 ✓ Get config for unknown supplier returns 404")
    void getConfig_unknownSupplier_returns404() throws Exception {
        GET(BASE + "/configs/nonexistent-supplier?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("BC-2202 ✓ Upsert config — second save updates existing")
    void upsertConfig_updatesExisting() throws Exception {
        String supplierId = createSupplier();

        POST(BASE + "/configs", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "apiUrl", "https://old.example",
                "enabled", false
        ), bearer("admin1")).andExpect(status().isCreated());

        POST(BASE + "/configs", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "apiUrl", "https://new.example",
                "enabled", true
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiUrl").value("https://new.example"))
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
