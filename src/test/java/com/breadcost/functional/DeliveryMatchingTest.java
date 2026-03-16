package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1305: Delivery matching against PO
 */
@DisplayName("R2 :: BC-1305 — Delivery Matching Against PO")
class DeliveryMatchingTest extends FunctionalTestBase {

    private String createApprovedPO(String ingredientId, double qty) throws Exception {
        String supplierBody = POST("/v2/suppliers", Map.of(
                "tenantId", TENANT, "name", "DelivSupplier-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(supplierBody).get("supplierId").asText();

        String poBody = POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "lines", List.of(
                        Map.of("ingredientId", ingredientId, "ingredientName", "Test Ingredient",
                                "qty", qty, "unit", "kg",
                                "unitPrice", 3.0, "currency", "USD")
                )
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String poId = om.readTree(poBody).get("po").get("poId").asText();

        PUT("/v2/purchase-orders/" + poId + "/approve?tenantId=" + TENANT, Map.of(), bearer("admin1"));
        return poId;
    }

    @Test
    @DisplayName("BC-1305 ✓ Match delivery returns 201 with delivery record")
    void matchDelivery_returns201() throws Exception {
        String ingId = "ing-" + UUID.randomUUID();
        String poId = createApprovedPO(ingId, 100.0);

        POST("/v2/purchase-orders/" + poId + "/deliveries", Map.of(
                "tenantId", TENANT,
                "notes", "Delivery arrived",
                "lines", List.of(
                        Map.of("ingredientId", ingId, "ingredientName", "Test Ingredient",
                                "qtyReceived", 100.0, "unit", "kg")
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivery.deliveryId").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1305 ✓ Exact match → no discrepancy")
    void matchDelivery_exactQty_noDiscrepancy() throws Exception {
        String ingId = "ing-" + UUID.randomUUID();
        String poId = createApprovedPO(ingId, 50.0);

        POST("/v2/purchase-orders/" + poId + "/deliveries", Map.of(
                "tenantId", TENANT,
                "lines", List.of(
                        Map.of("ingredientId", ingId, "qtyReceived", 50.0, "unit", "kg")
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivery.hasDiscrepancy").value(false))
                .andExpect(jsonPath("$.delivery.status").value("MATCHED"));
    }

    @Test
    @DisplayName("BC-1305 ✓ Partial delivery → discrepancy flagged")
    void matchDelivery_partialQty_discrepancyFlagged() throws Exception {
        String ingId = "ing-" + UUID.randomUUID();
        String poId = createApprovedPO(ingId, 100.0);

        POST("/v2/purchase-orders/" + poId + "/deliveries", Map.of(
                "tenantId", TENANT,
                "lines", List.of(
                        Map.of("ingredientId", ingId, "qtyReceived", 75.0, "unit", "kg")
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivery.hasDiscrepancy").value(true))
                .andExpect(jsonPath("$.delivery.status").value("DISCREPANCY"));
    }

    @Test
    @DisplayName("BC-1305 ✓ After matching, PO status becomes RECEIVED")
    void matchDelivery_poStatusBecomesReceived() throws Exception {
        String ingId = "ing-" + UUID.randomUUID();
        String poId = createApprovedPO(ingId, 30.0);

        POST("/v2/purchase-orders/" + poId + "/deliveries", Map.of(
                "tenantId", TENANT,
                "lines", List.of(
                        Map.of("ingredientId", ingId, "qtyReceived", 30.0)
                )
        ), bearer("admin1")).andExpect(status().isCreated());

        GET("/v2/purchase-orders/" + poId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.po.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("BC-1305 ✓ Delivery lines include discrepancy notes")
    void matchDelivery_linesHaveDiscrepancyNote() throws Exception {
        String ingId = "ing-" + UUID.randomUUID();
        String poId = createApprovedPO(ingId, 200.0);

        POST("/v2/purchase-orders/" + poId + "/deliveries", Map.of(
                "tenantId", TENANT,
                "lines", List.of(
                        Map.of("ingredientId", ingId, "qtyReceived", 150.0)
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].discrepancy").value(true))
                .andExpect(jsonPath("$.lines[0].discrepancyNote").isNotEmpty());
    }
}
