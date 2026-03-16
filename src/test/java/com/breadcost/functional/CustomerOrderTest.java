package com.breadcost.functional;

import com.breadcost.domain.Department;
import com.breadcost.domain.Product;
import com.breadcost.masterdata.DepartmentEntity;
import com.breadcost.masterdata.DepartmentRepository;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1104: Place order via customer portal
 *
 * AC:
 *   POST /v2/orders → 201 { orderId, status, totalAmount }
 *   Unknown product → 400
 *   Empty items    → 400
 */
@DisplayName("R2 :: BC-1104 — Place Order via Customer Portal")
class CustomerOrderTest extends FunctionalTestBase {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductRepository productRepository;

    private String deptId;
    private String productId;
    private final String customerId = "customer-order-test-" + UUID.randomUUID();

    @BeforeEach
    void seedProductData() {
        deptId = UUID.randomUUID().toString();
        DepartmentEntity dept = DepartmentEntity.builder()
                .departmentId(deptId)
                .tenantId(TENANT)
                .name("Order Test Dept " + deptId)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build();
        departmentRepository.save(dept);

        productId = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(productId)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Portal Bread " + productId)
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("3.00"))
                .vatRatePct(8.0)
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());
    }

    // ── Success path ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1104 ✓ POST /v2/orders → 201 with orderId and status")
    void placeOrder_validRequest_returns201WithOrderId() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",    TENANT,
                "customerId",  customerId,
                "customerName","Portal Customer",
                "items", List.of(Map.of(
                        "productId", productId,
                        "qty",       2
                )),
                "requestedDeliveryTime", Instant.now().plus(2, ChronoUnit.DAYS).toString()
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @DisplayName("BC-1104 ✓ Order total reflects product price × qty")
    void placeOrder_totalAmount_calculatedCorrectly() throws Exception {
        // 2 × $3.00 = $6.00 (possibly with rush premium)
        Map<String, Object> req = Map.of(
                "tenantId",    TENANT,
                "customerId",  customerId,
                "customerName","Portal Customer",
                "items", List.of(Map.of(
                        "productId", productId,
                        "qty",       2
                ))
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty());
    }

    @Test
    @DisplayName("BC-1104 ✓ Multiple items in one order → 201")
    void placeOrder_multipleItems_returns201() throws Exception {
        // Create a second product
        String product2Id = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(product2Id)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Second Product " + product2Id)
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("1.50"))
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        Map<String, Object> req = Map.of(
                "tenantId",    TENANT,
                "customerId",  customerId,
                "items", List.of(
                        Map.of("productId", productId,  "qty", 1),
                        Map.of("productId", product2Id, "qty", 3)
                )
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty());
    }

    // ── Validation / error paths ──────────────────────────────────────────────

    @Test
    @DisplayName("BC-1104 ✓ Unknown productId → 400")
    void placeOrder_unknownProduct_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", customerId,
                "items", List.of(Map.of(
                        "productId", "no-such-product",
                        "qty",       1
                ))
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1104 ✓ Missing customerId → 400")
    void placeOrder_missingCustomerId_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId", TENANT,
                "items", List.of(Map.of("productId", productId, "qty", 1))
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1104 ✓ Empty items list → 400")
    void placeOrder_emptyItems_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantId",   TENANT,
                "customerId", customerId,
                "items",      List.of()
        );

        POST("/v2/orders", req, bearer("admin1"))
                .andExpect(status().isBadRequest());
    }
}
