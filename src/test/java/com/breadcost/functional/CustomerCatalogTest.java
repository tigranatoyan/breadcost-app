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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for BC-1103: Customer-facing product catalog
 *
 * AC:
 *   GET /v2/products?tenantId=...         → 200 list of active products
 *   GET /v2/products/{id}?tenantId=...    → 200 single product
 *   Inactive product filtered out
 *   Unknown product                       → 400
 */
@DisplayName("R2 :: BC-1103 — Customer-facing Product Catalog")
class CustomerCatalogTest extends FunctionalTestBase {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductRepository productRepository;

    private String deptId;
    private String activeProduct1Id;
    private String activeProduct2Id;
    private String inactiveProductId;

    @BeforeEach
    void seedProducts() {
        // Create a department
        deptId = UUID.randomUUID().toString();
        DepartmentEntity dept = DepartmentEntity.builder()
                .departmentId(deptId)
                .tenantId(TENANT)
                .name("Catalog Dept " + deptId)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build();
        departmentRepository.save(dept);

        // Active product 1
        activeProduct1Id = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(activeProduct1Id)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Sourdough Loaf")
                .description("Classic sourdough bread")
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("4.50"))
                .vatRatePct(8.0)
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        // Active product 2
        activeProduct2Id = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(activeProduct2Id)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Baguette")
                .description("French baguette")
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("2.00"))
                .vatRatePct(8.0)
                .status(Product.ProductStatus.ACTIVE)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());

        // Inactive product — must NOT appear in catalog
        inactiveProductId = UUID.randomUUID().toString();
        productRepository.save(ProductEntity.builder()
                .productId(inactiveProductId)
                .tenantId(TENANT)
                .departmentId(deptId)
                .name("Discontinued Cake")
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("PC")
                .price(new BigDecimal("9.99"))
                .status(Product.ProductStatus.DISCONTINUED)
                .createdAtUtc(Instant.now())
                .updatedAtUtc(Instant.now())
                .build());
    }

    // ── Catalog list ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1103 ✓ GET /v2/products → 200 with active products only")
    void listCatalog_returnsActiveProductsOnly() throws Exception {
        GET("/v2/products?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[*].productId", hasItems(activeProduct1Id, activeProduct2Id)))
                .andExpect(jsonPath("$.content[*].productId", not(hasItem(inactiveProductId))));
    }

    @Test
    @DisplayName("BC-1103 ✓ Catalog response includes name, price, saleUnit")
    void listCatalog_responseFields_complete() throws Exception {
        GET("/v2/products?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.productId=='" + activeProduct1Id + "')].name",
                        hasItem("Sourdough Loaf")))
                .andExpect(jsonPath("$.content[?(@.productId=='" + activeProduct1Id + "')].price",
                        hasItem(4.5)))
                .andExpect(jsonPath("$.content[?(@.productId=='" + activeProduct1Id + "')].saleUnit",
                        hasItem("PIECE")));
    }

    @Test
    @DisplayName("BC-1103 ✓ Catalog for unknown tenant → 200 empty list")
    void listCatalog_unknownTenant_returnsEmpty() throws Exception {
        GET("/v2/products?tenantId=no-such-tenant", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ── Single product detail ─────────────────────────────────────────────────

    @Test
    @DisplayName("BC-1103 ✓ GET /v2/products/{id} → 200 product detail")
    void getProduct_activeProduct_returns200() throws Exception {
        GET("/v2/products/" + activeProduct2Id + "?tenantId=" + TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(activeProduct2Id)))
                .andExpect(jsonPath("$.name", is("Baguette")))
                .andExpect(jsonPath("$.price", is(2.0)));
    }

    @Test
    @DisplayName("BC-1103 ✓ GET /v2/products/{id} for inactive product → 400")
    void getProduct_inactiveProduct_returns400() throws Exception {
        GET("/v2/products/" + inactiveProductId + "?tenantId=" + TENANT, "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BC-1103 ✓ GET /v2/products/{id} for unknown id → 400")
    void getProduct_unknownId_returns400() throws Exception {
        GET("/v2/products/no-such-id?tenantId=" + TENANT, "")
                .andExpect(status().isBadRequest());
    }
}
