package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1306: FX rate per purchase transaction
 */
@DisplayName("R2 :: BC-1306 — FX Rate Per Purchase Transaction")
class POFxRateTest extends FunctionalTestBase {

    private String createSupplier() throws Exception {
        String body = POST("/v2/suppliers", Map.of(
                "tenantId", TENANT, "name", "FxSupplier-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("supplierId").asText();
    }

    @Test
    @DisplayName("BC-1306 ✓ PO stores fxRate and fxCurrencyCode")
    void createPO_fxRateStored() throws Exception {
        String supplierId = createSupplier();

        POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "fxRate", 1.35,
                "fxCurrencyCode", "GBP",
                "lines", List.of()
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.po.fxRate").value(1.35))
                .andExpect(jsonPath("$.po.fxCurrencyCode").value("GBP"));
    }

    @Test
    @DisplayName("BC-1306 ✓ Default fxRate is 1.0 when not provided")
    void createPO_defaultFxRate() throws Exception {
        String supplierId = createSupplier();

        POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.po.fxRate").value(1.0));
    }

    @Test
    @DisplayName("BC-1306 ✓ Multiple currencies supported on same tenant")
    void createPO_multiCurrency() throws Exception {
        String supplierId = createSupplier();

        for (String[] pair : new String[][]{{"1.35", "GBP"}, {"1.08", "EUR"}, {"0.73", "JPY"}}) {
            POST("/v2/purchase-orders", Map.of(
                    "tenantId", TENANT,
                    "supplierId", supplierId,
                    "fxRate", Double.parseDouble(pair[0]),
                    "fxCurrencyCode", pair[1]
            ), bearer("admin1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.po.fxCurrencyCode").value(pair[1]));
        }
    }

    @Test
    @DisplayName("BC-1306 ✓ FX rate persists after approval")
    void approvePO_fxRateRemains() throws Exception {
        String supplierId = createSupplier();

        String body = POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "fxRate", 4.20,
                "fxCurrencyCode", "PLN"
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String poId = om.readTree(body).get("po").get("poId").asText();

        PUT("/v2/purchase-orders/" + poId + "/approve?tenantId=" + TENANT,
                Map.of("approvedBy", "mgr"), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fxRate").value(4.20))
                .andExpect(jsonPath("$.fxCurrencyCode").value("PLN"));
    }

    @Test
    @DisplayName("BC-1306 ✓ FX rate visible when listing POs")
    void listPOs_fxRateVisible() throws Exception {
        String supplierId = createSupplier();

        POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "fxRate", 2.50,
                "fxCurrencyCode", "AED"
        ), bearer("admin1")).andExpect(status().isCreated());

        GET("/v2/purchase-orders?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fxCurrencyCode", hasItem("AED")));
    }
}
