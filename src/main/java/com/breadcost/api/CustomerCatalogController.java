package com.breadcost.api;

import com.breadcost.domain.Product;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer Portal — Product Catalog API
 *
 * BC-1103: Customer-facing product catalog
 *   GET /v2/products?tenantId=...          → list active products (public)
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

    // ── DTO ───────────────────────────────────────────────────────────────────

    /** Slim read-model exposed to end-customers (no cost / recipe internals). */
    public record CatalogProduct(
            String productId,
            String name,
            String description,
            String saleUnit,
            BigDecimal price,
            double vatRatePct
    ) {
        static CatalogProduct from(ProductEntity e) {
            return new CatalogProduct(
                    e.getProductId(),
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
     * BC-1103: List active products for a tenant.
     * GET /v2/products?tenantId=...
     */
    @GetMapping
    public ResponseEntity<List<CatalogProduct>> listCatalog(@RequestParam String tenantId) {
        List<CatalogProduct> catalog = productRepository
                .findByTenantIdAndStatus(tenantId, Product.ProductStatus.ACTIVE)
                .stream()
                .map(CatalogProduct::from)
                .toList();

        log.debug("Catalog requested: tenantId={} products={}", tenantId, catalog.size());
        return ResponseEntity.ok(catalog);
    }

    /**
     * BC-1103: Get a specific product detail.
     * GET /v2/products/{id}?tenantId=...
     *
     * Returns 400 if the product is not found or not active for this tenant.
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
