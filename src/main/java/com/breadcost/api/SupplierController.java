package com.breadcost.api;

import com.breadcost.supplier.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import com.breadcost.subscription.SubscriptionRequired;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Supplier REST API — BC-1301: Supplier catalog CRUD
 *
 * POST   /v2/suppliers                       — create supplier
 * GET    /v2/suppliers?tenantId=...          — list suppliers
 * GET    /v2/suppliers/{id}?tenantId=...     — get supplier
 * PUT    /v2/suppliers/{id}?tenantId=...     — update supplier
 * DELETE /v2/suppliers/{id}?tenantId=...     — delete supplier
 * POST   /v2/suppliers/{id}/catalog?tenantId=... — add catalog item
 * GET    /v2/suppliers/{id}/catalog?tenantId=... — list catalog items
 */
@Tag(name = "Suppliers", description = "Supplier master data and catalog management")
@RestController
@RequestMapping("/v2/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager','Warehouse')")
@SubscriptionRequired("SUPPLIER")
public class SupplierController {

    private final SupplierService supplierService;

    // ── Request DTOs ──────────────────────────────────────────────────────────

    @Data
    public static class CreateSupplierRequest {
        @NotBlank private String tenantId;
        @NotBlank private String name;
        private String contactEmail;
        private String contactPhone;
        private String notes;
    }

    @Data
    public static class UpdateSupplierRequest {
        private String name;
        private String contactEmail;
        private String contactPhone;
        private String notes;
    }

    @Data
    public static class AddCatalogItemRequest {
        @NotBlank private String tenantId;
        @NotBlank private String ingredientId;
        private String ingredientName;
        private BigDecimal unitPrice;
        private String currency;
        private int leadTimeDays = 1;
        private double moq = 1.0;
        private String unit;
    }

    // ── Supplier CRUD ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SupplierEntity> createSupplier(
            @Valid @RequestBody CreateSupplierRequest req) {
        SupplierEntity supplier = supplierService.createSupplier(
                req.getTenantId(), req.getName(),
                req.getContactEmail(), req.getContactPhone(), req.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }

    @GetMapping
    public ResponseEntity<List<SupplierEntity>> listSuppliers(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(supplierService.listSuppliers(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierEntity> getSupplier(
            @PathVariable("id") String supplierId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(supplierService.getSupplier(tenantId, supplierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierEntity> updateSupplier(
            @PathVariable("id") String supplierId,
            @RequestParam String tenantId,
            @RequestBody UpdateSupplierRequest req) {
        return ResponseEntity.ok(supplierService.updateSupplier(
                tenantId, supplierId,
                req.getName(), req.getContactEmail(),
                req.getContactPhone(), req.getNotes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable("id") String supplierId,
            @RequestParam String tenantId) {
        supplierService.deleteSupplier(tenantId, supplierId);
        return ResponseEntity.noContent().build();
    }

    // ── Catalog items ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/catalog")
    public ResponseEntity<SupplierCatalogItemEntity> addCatalogItem(
            @PathVariable("id") String supplierId,
            @Valid @RequestBody AddCatalogItemRequest req) {
        SupplierCatalogItemEntity item = supplierService.addCatalogItem(
                new SupplierService.CatalogItemRequest(
                        req.getTenantId(), supplierId,
                        req.getIngredientId(), req.getIngredientName(),
                        req.getUnitPrice(), req.getCurrency(),
                        req.getLeadTimeDays(), req.getMoq(), req.getUnit()));
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/{id}/catalog")
    public ResponseEntity<List<SupplierCatalogItemEntity>> getCatalogItems(
            @PathVariable("id") String supplierId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(supplierService.getCatalogItems(tenantId, supplierId));
    }
}
