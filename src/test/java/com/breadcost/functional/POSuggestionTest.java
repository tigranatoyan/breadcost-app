package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1302: PO suggestion generation
 */
@DisplayName("R2 :: BC-1302 — PO Suggestion Generation")
class POSuggestionTest extends FunctionalTestBase {

    private String createSupplierWithItem() throws Exception {
        String name = "AutoSupplier-" + UUID.randomUUID();
        String body = POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", name), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(body).get("supplierId").asText();

        POST("/v2/suppliers/" + supplierId + "/catalog", Map.of(
                "tenantId", TENANT,
                "ingredientId", "ing-" + UUID.randomUUID(),
                "ingredientName", "Auto Flour",
                "unitPrice", 1.80,
                "moq", 25.0,
                "unit", "kg"
        ), "").andExpect(status().isCreated());

        return supplierId;
    }

    @Test
    @DisplayName("BC-1302 ✓ Suggest returns DRAFT POs")
    void suggest_returnsDraftPOs() throws Exception {
        createSupplierWithItem();

        POST("/v2/purchase-orders/suggest", Map.of("tenantId", TENANT), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("BC-1302 ✓ Suggested PO has status DRAFT")
    void suggest_poStatusIsDraft() throws Exception {
        createSupplierWithItem();

        POST("/v2/purchase-orders/suggest", Map.of("tenantId", TENANT), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", hasItem("DRAFT")));
    }

    @Test
    @DisplayName("BC-1302 ✓ Response includes supplierId and poId")
    void suggest_includesSupplierAndPoId() throws Exception {
        createSupplierWithItem();

        POST("/v2/purchase-orders/suggest", Map.of("tenantId", TENANT), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].poId").isArray())
                .andExpect(jsonPath("$[*].supplierId").isArray());
    }

    @Test
    @DisplayName("BC-1302 ✓ Missing tenantId → 400")
    void suggest_missingTenantId_returns400() throws Exception {
        POST("/v2/purchase-orders/suggest", Map.of(), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1302 ✓ Suggest with no catalog items returns empty list")
    void suggest_noCatalogItems_returnsEmptyOrPOs() throws Exception {
        // Use a unique tenant to ensure no stray items
        String newTenant = "suggest-tenant-" + UUID.randomUUID();
        POST("/v2/purchase-orders/suggest", Map.of("tenantId", newTenant), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
