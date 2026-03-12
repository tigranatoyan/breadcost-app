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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2901: Catalog search, filter, pagination.
 *
 * AC:
 *   GET /v2/products?tenantId=&search=&departmentId=&page=0&size=20&sort=name,asc
 *   search: case-insensitive LIKE on product name
 *   departmentId: filters by department
 *   Pagination: Page with totalElements, totalPages
 *   Only active products
 *   Sort options: name, price
 */
@DisplayName("R4-S2 :: BC-2901 — Catalog Search, Filter, Pagination")
class CatalogSearchTest extends FunctionalTestBase {

    private static final String CAT_TENANT = "catalog-search-tenant";

    @Autowired private ProductRepository productRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private String deptBread;
    private String deptPastry;

    @BeforeEach
    void seedCatalog() {
        // Use a dedicated tenant to isolate from other test classes' data
        if (departmentRepository.findByTenantId(CAT_TENANT).isEmpty()) {
            deptBread = createDept("Bread");
            deptPastry = createDept("Pastry");

            // 5 bread products (4 active + 1 discontinued)
            createProduct(deptBread, "White Bread", new BigDecimal("500"), Product.ProductStatus.ACTIVE);
            createProduct(deptBread, "Whole Wheat Bread", new BigDecimal("650"), Product.ProductStatus.ACTIVE);
            createProduct(deptBread, "Rye Bread", new BigDecimal("700"), Product.ProductStatus.ACTIVE);
            createProduct(deptBread, "Sourdough Bread", new BigDecimal("900"), Product.ProductStatus.ACTIVE);
            createProduct(deptBread, "Discontinued Bread", new BigDecimal("300"), Product.ProductStatus.DISCONTINUED);

            // 3 pastry products
            createProduct(deptPastry, "Croissant", new BigDecimal("400"), Product.ProductStatus.ACTIVE);
            createProduct(deptPastry, "Danish Pastry", new BigDecimal("550"), Product.ProductStatus.ACTIVE);
            createProduct(deptPastry, "Cinnamon Roll", new BigDecimal("450"), Product.ProductStatus.ACTIVE);
        } else {
            deptBread = departmentRepository.findByTenantId(CAT_TENANT).stream()
                    .filter(d -> d.getName().equals("Bread")).findFirst()
                    .map(DepartmentEntity::getDepartmentId).orElse("");
            deptPastry = departmentRepository.findByTenantId(CAT_TENANT).stream()
                    .filter(d -> d.getName().equals("Pastry")).findFirst()
                    .map(DepartmentEntity::getDepartmentId).orElse("");
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ search returns filtered set (case-insensitive)")
    void search_byName_returnsFiltered() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&search=bread", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))  // 4 active breads
                .andExpect(jsonPath("$.totalElements", is(4)));
    }

    @Test
    @DisplayName("BC-2901 ✓ search is case insensitive")
    void search_caseInsensitive() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&search=SOURDOUGH", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Sourdough Bread")));
    }

    @Test
    @DisplayName("BC-2901 ✓ search returns empty for no match")
    void search_noMatch_returnsEmpty() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&search=pizza", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ── Filter by department ──────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ filter by departmentId")
    void filter_byDepartment() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&departmentId=" + deptPastry, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))  // 3 pastry
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    @DisplayName("BC-2901 ✓ combined search + department filter")
    void search_andFilter() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&search=bread&departmentId=" + deptBread, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)));  // all 4 active bread
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ pagination works (page 0, size 3)")
    void pagination_firstPage() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&page=0&size=3&sort=name,asc", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(7)))  // 7 active total
                .andExpect(jsonPath("$.totalPages", is(3)))     // ceil(7/3)
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    @DisplayName("BC-2901 ✓ pagination page 2 (last page)")
    void pagination_lastPage() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&page=2&size=3&sort=name,asc", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))  // 7 mod 3 = 1
                .andExpect(jsonPath("$.totalElements", is(7)))
                .andExpect(jsonPath("$.page", is(2)));
    }

    @Test
    @DisplayName("BC-2901 ✓ empty page returns empty content")
    void pagination_beyondLastPage() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&page=10&size=3", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(7)));
    }

    // ── Sort ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ sort by name ascending")
    void sort_nameAsc() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&sort=name,asc&size=100", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Cinnamon Roll")))
                .andExpect(jsonPath("$.content[6].name", is("Whole Wheat Bread")));
    }

    @Test
    @DisplayName("BC-2901 ✓ sort by price descending")
    void sort_priceDesc() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&sort=price,desc&size=100", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Sourdough Bread")));  // 900
    }

    // ── Only active products ──────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ discontinued products excluded")
    void filter_excludesDiscontinued() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT + "&search=discontinued", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ── Defaults ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BC-2901 ✓ no search/filter returns all active with pagination metadata")
    void noFilters_returnsAll() throws Exception {
        GET("/v2/products?tenantId=" + CAT_TENANT, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", is(7)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createDept(String name) {
        DepartmentEntity dept = DepartmentEntity.builder()
                .departmentId(UUID.randomUUID().toString())
                .tenantId(CAT_TENANT)
                .name(name)
                .leadTimeHours(4)
                .warehouseMode(Department.WarehouseMode.SHARED)
                .status(Department.DepartmentStatus.ACTIVE)
                .build();
        return departmentRepository.save(dept).getDepartmentId();
    }

    private void createProduct(String deptId, String name, BigDecimal price,
                               Product.ProductStatus status) {
        ProductEntity p = ProductEntity.builder()
                .productId(UUID.randomUUID().toString())
                .tenantId(CAT_TENANT)
                .departmentId(deptId)
                .name(name)
                .description(name + " description")
                .saleUnit(Product.SaleUnit.PIECE)
                .baseUom("pc")
                .price(price)
                .vatRatePct(20.0)
                .status(status)
                .build();
        productRepository.save(p);
    }
}
