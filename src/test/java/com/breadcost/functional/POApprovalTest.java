package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-1303: PO review and approval
 */
@DisplayName("R2 :: BC-1303 — PO Review and Approval")
class POApprovalTest extends FunctionalTestBase {

    private String createSupplier() throws Exception {
        String body = POST("/v2/suppliers", Map.of(
                "tenantId", TENANT,
                "name", "ApprovalSupplier-" + UUID.randomUUID()
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("supplierId").asText();
    }

    private String createPO(String supplierId) throws Exception {
        String body = POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "notes", "Test PO",
                "fxRate", 1.0,
                "fxCurrencyCode", "USD",
                "lines", List.of(
                        Map.of("ingredientId", "ing-001", "ingredientName", "Flour",
                                "qty", 100.0, "unit", "kg",
                                "unitPrice", 2.50, "currency", "USD")
                )
        ), bearer("admin1")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("po").get("poId").asText();
    }

    @Test
    @DisplayName("BC-1303 ✓ Create PO returns 201 with PENDING_APPROVAL status")
    void createPO_returns201_pendingApproval() throws Exception {
        String supplierId = createSupplier();

        POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "lines", List.of(
                        Map.of("ingredientId", "ing-001", "qty", 50.0,
                                "unitPrice", 3.0, "currency", "USD")
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.po.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.po.poId").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1303 ✓ Approve PO changes status to APPROVED")
    void approvePO_changesStatusToApproved() throws Exception {
        String supplierId = createSupplier();
        String poId = createPO(supplierId);

        PUT("/v2/purchase-orders/" + poId + "/approve?tenantId=" + TENANT,
                Map.of("approvedBy", "manager@breadcost.com"), bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("manager@breadcost.com"))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1303 ✓ Get PO returns detail with lines")
    void getPO_returnsDetailWithLines() throws Exception {
        String supplierId = createSupplier();
        String poId = createPO(supplierId);

        GET("/v2/purchase-orders/" + poId + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.po.poId").value(poId))
                .andExpect(jsonPath("$.lines").isArray());
    }

    @Test
    @DisplayName("BC-1303 ✓ PO line includes totalAmount calculation")
    void createPO_totalAmountCalculated() throws Exception {
        String supplierId = createSupplier();

        POST("/v2/purchase-orders", Map.of(
                "tenantId", TENANT,
                "supplierId", supplierId,
                "lines", List.of(
                        Map.of("ingredientId", "ing-a", "qty", 10.0, "unitPrice", 5.0, "currency", "USD")
                )
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.po.totalAmount").value(50.0));
    }

    @Test
    @DisplayName("BC-1303 ✓ List POs returns all tenant POs")
    void listPOs_returnsAll() throws Exception {
        String supplierId = createSupplier();
        createPO(supplierId);

        GET("/v2/purchase-orders?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("BC-1303 ✓ Approve non-existent PO returns 400")
    void approvePO_notFound_returns400() throws Exception {
        PUT("/v2/purchase-orders/nonexistent-po/approve?tenantId=" + TENANT,
                Map.of(), bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
