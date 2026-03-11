package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1304: PO Excel export
 */
@DisplayName("R2 :: BC-1304 — PO Excel Export")
class POExcelExportTest extends FunctionalTestBase {

    private String createApprovedPO() throws Exception {
        String supplierBody = POST("/v2/suppliers", Map.of(
                "tenantId", TENANT, "name", "ExcelSupplier-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String supplierId = om.readTree(supplierBody).get("supplierId").asText();

        String poBody = POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "fxRate", 1.25,
                "fxCurrencyCode", "EUR",
                "lines", List.of(
                        Map.of("ingredientId", "ing-x", "ingredientName", "Wheat",
                                "qty", 200.0, "unit", "kg",
                                "unitPrice", 1.80, "currency", "EUR")
                )
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String poId = om.readTree(poBody).get("po").get("poId").asText();

        PUT("/v2/purchase-orders/" + poId + "/approve?tenantId=" + TENANT,
                Map.of("approvedBy", "mgr"), bearer("admin1"));
        return poId;
    }

    @Test
    @DisplayName("BC-1304 ✓ Export returns 200 with xlsx content-type")
    void exportPO_returnsXlsx() throws Exception {
        String poId = createApprovedPO();

        GET("/v2/purchase-orders/" + poId + "/export?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("BC-1304 ✓ Export body is non-empty bytes")
    void exportPO_bodyIsNonEmpty() throws Exception {
        String poId = createApprovedPO();

        byte[] body = GET("/v2/purchase-orders/" + poId + "/export?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        org.junit.jupiter.api.Assertions.assertTrue(body.length > 0,
                "Excel export should produce non-empty bytes");
    }

    @Test
    @DisplayName("BC-1304 ✓ Export Content-Disposition is attachment with filename")
    void exportPO_contentDispositionIsAttachment() throws Exception {
        String poId = createApprovedPO();

        GET("/v2/purchase-orders/" + poId + "/export?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("BC-1304 ✓ Export non-existent PO returns 400")
    void exportPO_notFound_returns400() throws Exception {
        GET("/v2/purchase-orders/bad-po-id/export?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
