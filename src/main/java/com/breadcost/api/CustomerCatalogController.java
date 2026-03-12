package com.breadcost.api;

import com.breadcost.domain.Product;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Customer Portal — Product Catalog API
 *
 * BC-1103 / BC-2901: Customer-facing product catalog with search, filter, pagination.
 *   GET /v2/products?tenantId=...&search=...&departmentId=...&page=0&size=20&sort=name,asc
 *   GET /v2/products/{id}?tenantId=...     → product detail (public)
 *
 * Read-only, unauthenticated (any-request-permitted by SecurityConfig).
 * Returns a slim CatalogProduct projection (no cost internals).
 */
@RestController
@RequestMapping("/v2/products")
@RequiredArgsConstructor
@Slf4j
public class CustomerCatalogController {

    private final ProductRepository productRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "price");

    // ── DTO ───────────────────────────────────────────────────────────────────

    /** Slim read-model exposed to end-customers (no cost / recipe internals). */
    public record CatalogProduct(
            String productId,
            String departmentId,
            String name,
            String description,
            String saleUnit,
            BigDecimal price,
            double vatRatePct
    ) {
        static CatalogProduct from(ProductEntity e) {
            return new CatalogProduct(
                    e.getProductId(),
                    e.getDepartmentId(),
                    e.getName(),
                    e.getDescription(),
                    e.getSaleUnit() != null ? e.getSaleUnit().name() : null,
                    e.getPrice(),
                    e.getVatRatePct()
            );
        }
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * BC-2901: Paginated catalog search with filter.
     * GET /v2/products?tenantId=...&search=...&departmentId=...&page=0&size=20&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listCatalog(
            @RequestParam String tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        Specification<ProductEntity> spec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), tenantId);
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), Product.ProductStatus.ACTIVE));

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern));
        }
        if (departmentId != null && !departmentId.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("departmentId"), departmentId));
        }

        String[] sortParts = sort.split(",");
        String sortField = ALLOWED_SORT_FIELDS.contains(sortParts[0]) ? sortParts[0] : "name";
        Sort.Direction dir = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(dir, sortField));

        Page<CatalogProduct> result = productRepository.findAll(spec, pageable)
                .map(CatalogProduct::from);

        log.debug("Catalog search: tenantId={} search={} dept={} page={} results={}",
                tenantId, search, departmentId, page, result.getTotalElements());

        return ResponseEntity.ok(Map.of(
                "content",       result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages",    result.getTotalPages(),
                "page",          result.getNumber(),
                "size",          result.getSize()
        ));
    }

    /**
     * BC-1103: Get a specific product detail.
     * GET /v2/products/{id}?tenantId=...
     */
    @GetMapping("/{id}")
    public ResponseEntity<CatalogProduct> getProduct(
            @PathVariable("id") String productId,
            @RequestParam String tenantId) {

        CatalogProduct product = productRepository.findById(productId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
                .map(CatalogProduct::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found or unavailable: " + productId));

        return ResponseEntity.ok(product);
    }
}
