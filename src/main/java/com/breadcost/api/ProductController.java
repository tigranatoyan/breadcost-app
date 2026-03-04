package com.breadcost.api;

import com.breadcost.domain.Product;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Product management (FR-4.1, FR-4.7, FR-4.8)
 */
@RestController
@RequestMapping("/v1/products")
@Slf4j
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    public record CreateProductRequest(
            @NotBlank String tenantId,
            @NotBlank String departmentId,
            @NotBlank String name,
            String description,
            @NotNull Product.SaleUnit saleUnit,
            @NotBlank String baseUom
    ) {}

    public record UpdateProductRequest(
            @NotBlank String name,
            String description,
            @NotNull Product.SaleUnit saleUnit,
            @NotBlank String baseUom,
            @NotNull Product.ProductStatus status
    ) {}

    /**
     * GET /v1/products?tenantId=xxx
     * List all products for a tenant
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<List<ProductEntity>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String departmentId) {
        if (departmentId != null) {
            return ResponseEntity.ok(productService.listByDepartment(tenantId, departmentId));
        }
        return ResponseEntity.ok(productService.listByTenant(tenantId));
    }

    /**
     * GET /v1/products/{productId}
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('Admin', 'Manager', 'Technologist', 'ProductionSupervisor')")
    public ResponseEntity<ProductEntity> get(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getById(productId));
    }

    /**
     * POST /v1/products
     * Create a new product (FR-4.1)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<ProductEntity> create(@Valid @RequestBody CreateProductRequest req) {
        ProductEntity created = productService.create(
                new ProductService.CreateProductRequest(
                        req.tenantId(), req.departmentId(), req.name(),
                        req.description(), req.saleUnit(), req.baseUom(), getPrincipalName()
                ));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /v1/products/{productId}
     * Update product details
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('Admin', 'Technologist')")
    public ResponseEntity<ProductEntity> update(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest req) {
        ProductEntity updated = productService.update(productId,
                new ProductService.UpdateProductRequest(
                        req.name(), req.description(), req.saleUnit(), req.baseUom(), req.status()
                ));
        return ResponseEntity.ok(updated);
    }

    private String getPrincipalName() {
        return "system";
    }
}
