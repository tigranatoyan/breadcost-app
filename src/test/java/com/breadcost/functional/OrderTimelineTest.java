package com.breadcost.functional;

import com.breadcost.domain.Department;
import com.breadcost.domain.Product;
import com.breadcost.masterdata.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2903: Order tracking timeline
 *
 * GET /v2/orders/{id}/timeline → [{status, timestamp, description}]
 * Records status transitions: DRAFT → CONFIRMED → IN_PRODUCTION → READY → DELIVERED
 */
@DisplayName("R4-S3 :: BC-2903 — Order Tracking Timeline")
class OrderTimelineTest extends FunctionalTestBase {

    private static final String TL_TENANT = "timeline-tenant";

    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProductRepository productRepository;

    private String productId;
    private final String customerId = "tl-customer-" + UUID.randomUUID();

    @BeforeEach
    void seedCatalog() {
        String deptId = UUID.randomUUID().toString();
        departmentRepository.save(DepartmentEntity.builder()
                .departmentId(deptId).tenantId(TL_TENANT)
                .name("TL Dept " + deptId).warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now()).updatedAtUtc(Instant.now()).build());

        productId = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(productId).tenantId(TL_TENANT).departmentId(deptId)
                .name("TL Bread").saleUnit(Product.SaleUnit.PIECE).baseUom("PC")
                .price(new BigDecimal("5.00")).status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now()).updatedAtUtc(Instant.now()).build());
    }

    /** Helper: place order via v2 endpoint and return orderId */
    private String placeOrder() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TL_TENANT, "customerId", customerId,
                "items", List.of(Map.of("productId", productId, "qty", 1)));
        String body = POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("orderId").asText();
    }

    @Test
    @DisplayName("BC-2903 ✓ New order has DRAFT in timeline")
    void newOrder_timelineContainsDraft() throws Exception {
        String orderId = placeOrder();
        GET("/v2/orders/" + orderId + "/timeline?tenantId=" + TL_TENANT + "&customerId=" + customerId,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("DRAFT")));
    }

    @Test
    @DisplayName("BC-2903 ✓ Full lifecycle yields all transitions")
    void fullLifecycle_allTransitionsRecorded() throws Exception {
        String orderId = placeOrder();

        // DRAFT → CONFIRMED
        POST_noBody("/v1/orders/" + orderId + "/confirm?tenantId=" + TL_TENANT, bearer("admin1"))
                .andExpect(status().isOk());
        // CONFIRMED → IN_PRODUCTION
        POST_noBody("/v1/orders/" + orderId + "/status?tenantId=" + TL_TENANT + "&targetStatus=IN_PRODUCTION",
                bearer("admin1")).andExpect(status().isOk());
        // IN_PRODUCTION → READY
        POST_noBody("/v1/orders/" + orderId + "/status?tenantId=" + TL_TENANT + "&targetStatus=READY",
                bearer("admin1")).andExpect(status().isOk());
        // READY → DELIVERED
        POST_noBody("/v1/orders/" + orderId + "/status?tenantId=" + TL_TENANT + "&targetStatus=DELIVERED",
                bearer("admin1")).andExpect(status().isOk());

        GET("/v2/orders/" + orderId + "/timeline?tenantId=" + TL_TENANT + "&customerId=" + customerId,
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].status", is("DRAFT")))
                .andExpect(jsonPath("$[1].status", is("CONFIRMED")))
                .andExpect(jsonPath("$[2].status", is("IN_PRODUCTION")))
                .andExpect(jsonPath("$[3].status", is("READY")))
                .andExpect(jsonPath("$[4].status", is("DELIVERED")))
                .andExpect(jsonPath("$[0].timestamp").isNumber())
                .andExpect(jsonPath("$[0].description").isString());
    }

    @Test
    @DisplayName("BC-2903 ✓ Unknown order returns 400")
    void unknownOrder_returns400() throws Exception {
        GET("/v2/orders/nonexistent/timeline?tenantId=" + TL_TENANT + "&customerId=" + customerId,
                bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-2903 ✓ Wrong customer cannot see timeline")
    void wrongCustomer_returns400() throws Exception {
        String orderId = placeOrder();
        GET("/v2/orders/" + orderId + "/timeline?tenantId=" + TL_TENANT + "&customerId=wrong-customer",
                bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
