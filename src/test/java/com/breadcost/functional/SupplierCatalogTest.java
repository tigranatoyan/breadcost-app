package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1301: Supplier catalog CRUD
 */
@DisplayName("R2 :: BC-1301 — Supplier Catalog CRUD")
class SupplierCatalogTest extends FunctionalTestBase {

    @Test
    @DisplayName("BC-1301 ✓ Create supplier returns 201 with supplierId")
    void createSupplier_returns201() throws Exception {
        POST("/v2/suppliers", Map.of(
                "tenantId", TENANT,
                "name", "Best Flour Co-" + UUID.randomUUID(),
                "contactEmail", "contact@flour.co",
                "contactPhone", "+1-555-1234"
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").isNotEmpty())
                .andExpect(jsonPath("$.name").value(containsString("Best Flour Co")));
    }

    @Test
    @DisplayName("BC-1301 ✓ List suppliers returns created supplier")
    void listSuppliers_returnsCreated() throws Exception {
        String name = "Yeast Masters-" + UUID.randomUUID();
        POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", name), "")
                .andExpect(status().isCreated());

        GET("/v2/suppliers?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    @DisplayName("BC-1301 ✓ Update supplier name via PUT")
    void updateSupplier_changesName() throws Exception {
        String name = "OldName-" + UUID.randomUUID();
        String body = POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", name), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String supplierId = om.readTree(body).get("supplierId").asText();
        String newName = "NewName-" + UUID.randomUUID();

        PUT("/v2/suppliers/" + supplierId + "?tenantId=" + TENANT,
                Map.of("name", newName), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    @DisplayName("BC-1301 ✓ Add catalog item to supplier returns 201")
    void addCatalogItem_returns201() throws Exception {
        String body = POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", "Grain Co-" + UUID.randomUUID()), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(body).get("supplierId").asText();

        POST("/v2/suppliers/" + supplierId + "/catalog", Map.of(
                "tenantId", TENANT,
                "ingredientId", "ing-wheat-001",
                "ingredientName", "Wheat Flour",
                "unitPrice", 2.50,
                "currency", "USD",
                "leadTimeDays", 3,
                "moq", 50.0,
                "unit", "kg"
        ), "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").isNotEmpty())
                .andExpect(jsonPath("$.ingredientName").value("Wheat Flour"));
    }

    @Test
    @DisplayName("BC-1301 ✓ Get catalog items for supplier")
    void getCatalogItems_returnsList() throws Exception {
        String body = POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", "SeedHouse-" + UUID.randomUUID()), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(body).get("supplierId").asText();

        POST("/v2/suppliers/" + supplierId + "/catalog", Map.of(
                "tenantId", TENANT, "ingredientId", "ing-seed-001", "ingredientName", "Sesame Seeds"
        ), "").andExpect(status().isCreated());

        GET("/v2/suppliers/" + supplierId + "/catalog?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].ingredientName", hasItem("Sesame Seeds")));
    }

    @Test
    @DisplayName("BC-1301 ✓ Delete supplier returns 204")
    void deleteSupplier_returns204() throws Exception {
        String body = POST("/v2/suppliers", Map.of("tenantId", TENANT, "name", "TempSupplier-" + UUID.randomUUID()), "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(body).get("supplierId").asText();

        DELETE("/v2/suppliers/" + supplierId + "?tenantId=" + TENANT, "")
                .andExpect(status().isNoContent());
    }
}
