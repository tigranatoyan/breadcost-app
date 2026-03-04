package com.breadcost.functional;

import com.breadcost.domain.Department;
import com.breadcost.domain.Product;
import com.breadcost.masterdata.DepartmentEntity;
import com.breadcost.masterdata.DepartmentRepository;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Functional tests for BC-1105: Real-time order status and order history
 *
 * AC:
 *   GET /v2/orders/{id}?tenantId=...&customerId=...  → 200 with order status
 *   GET /v2/orders?tenantId=...&customerId=...       → 200 list
 *   Unknown orderId                                  → 400
 *   Wrong customerId                                 → 400
 */
@DisplayName("R2 :: BC-1105 — Order Status & History")
class CustomerOrderStatusTest extends FunctionalTestBase {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductRepository productRepository;

    private String productId;
    private final String customerId = "status-test-customer-" + UUID.randomUUID();

    @BeforeEach
    void seedCatalog() {
        String deptId = UUID.randomUUID().toString();
        departmentRepository.save(DepartmentEntity.builder()
                .departmentId(deptId)
                .tenantId(TENANT)
                .name("Status Test Dept " + deptId)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        productId = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(productId)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Status Test Bread")
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("2.50"))
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());
    }

    /** Helper: place an order and return the orderId */
    private String placeOrder() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", customerId,
                "items",      List.of(Map.of("productId", productId, "qty", 1))
        );
        String body = POST("/v2/orders", req, "")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = om.readTree(body);
        return json.get("orderId").asText();
    }

    // ── Order status ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1105 ✓ GET /v2/orders/{id} → 200 with correct orderId")
    void getOrderStatus_existingOrder_returns200() throws Exception {
        String orderId = placeOrder();

        GET("/v2/orders/" + orderId + "?tenantId=" + TENANT + "&customerId=" + customerId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(orderId)))
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1105 ✓ Order status reflects DRAFT/CONFIRMED state")
    void getOrderStatus_statusField_present() throws Exception {
        String orderId = placeOrder();

        GET("/v2/orders/" + orderId + "?tenantId=" + TENANT + "&customerId=" + customerId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("BC-1105 ✓ Unknown orderId → 400")
    void getOrderStatus_unknownId_returns400() throws Exception {
        GET("/v2/orders/no-such-order?tenantId=" + TENANT + "&customerId=" + customerId, "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1105 ✓ Wrong customerId cannot see order → 400")
    void getOrderStatus_wrongCustomer_returns400() throws Exception {
        String orderId = placeOrder();

        GET("/v2/orders/" + orderId + "?tenantId=" + TENANT + "&customerId=wrong-customer", "")
                .andExpect(status().isBadRequest());
    }

    // ── Order history ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1105 ✓ GET /v2/orders → 200 list of customer orders")
    void getOrderHistory_returnsCustomerOrders() throws Exception {
        String orderId1 = placeOrder();
        String orderId2 = placeOrder();

        GET("/v2/orders?tenantId=" + TENANT + "&customerId=" + customerId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].orderId", hasItems(orderId1, orderId2)));
    }

    @Test
    @DisplayName("BC-1105 ✓ Order history for unknown customer → 200 empty list")
    void getOrderHistory_unknownCustomer_returnsEmpty() throws Exception {
        GET("/v2/orders?tenantId=" + TENANT + "&customerId=no-such-customer", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
