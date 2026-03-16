package com.breadcost.supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Supplier service — BC-E13 (BC-1301)
 *
 * BC-1301: Supplier catalog CRUD
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private static final String SUPPLIER_NOT_FOUND = "Supplier not found: ";

    private final SupplierRepository supplierRepository;
    private final SupplierCatalogItemRepository catalogItemRepository;

    // ── BC-1301: Supplier CRUD ────────────────────────────────────────────────

    @Transactional
    public SupplierEntity createSupplier(String tenantId, String name,
                                         String contactEmail, String contactPhone,
                                         String notes) {
        supplierRepository.findByTenantIdAndName(tenantId, name).ifPresent(s -> {
            throw new IllegalArgumentException("Supplier already exists with name: " + name);
        });

        SupplierEntity supplier = SupplierEntity.builder()
                .supplierId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(name)
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .notes(notes)
                .build();

        log.info("Creating supplier: tenantId={} name={}", tenantId, name);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierEntity updateSupplier(String tenantId, String supplierId,
                                          String name, String contactEmail,
                                          String contactPhone, String notes) {
        SupplierEntity supplier = supplierRepository.findByTenantIdAndSupplierId(tenantId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException(SUPPLIER_NOT_FOUND + supplierId));

        if (name != null) supplier.setName(name);
        if (contactEmail != null) supplier.setContactEmail(contactEmail);
        if (contactPhone != null) supplier.setContactPhone(contactPhone);
        if (notes != null) supplier.setNotes(notes);

        return supplierRepository.save(supplier);
    }

    public List<SupplierEntity> listSuppliers(String tenantId) {
        return supplierRepository.findByTenantId(tenantId);
    }

    public SupplierEntity getSupplier(String tenantId, String supplierId) {
        return supplierRepository.findByTenantIdAndSupplierId(tenantId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException(SUPPLIER_NOT_FOUND + supplierId));
    }

    @Transactional
    public void deleteSupplier(String tenantId, String supplierId) {
        SupplierEntity supplier = supplierRepository.findByTenantIdAndSupplierId(tenantId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException(SUPPLIER_NOT_FOUND + supplierId));
        supplierRepository.delete(supplier);
    }

    // ── BC-1301: Catalog items ────────────────────────────────────────────────

    public record CatalogItemRequest(
            String tenantId, String supplierId,
            String ingredientId, String ingredientName,
            BigDecimal unitPrice, String currency,
            int leadTimeDays, double moq, String unit) {}

    @Transactional
    public SupplierCatalogItemEntity addCatalogItem(CatalogItemRequest req) {
        // Verify supplier exists
        getSupplier(req.tenantId(), req.supplierId());

        SupplierCatalogItemEntity item = SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .tenantId(req.tenantId())
                .supplierId(req.supplierId())
                .ingredientId(req.ingredientId())
                .ingredientName(req.ingredientName())
                .unitPrice(req.unitPrice() != null ? req.unitPrice() : BigDecimal.ZERO)
                .currency(req.currency() != null ? req.currency() : "USD")
                .leadTimeDays(req.leadTimeDays() > 0 ? req.leadTimeDays() : 1)
                .moq(req.moq() > 0 ? req.moq() : 1.0)
                .unit(req.unit())
                .build();

        log.info("Adding catalog item: tenantId={} supplierId={} ingredient={}", req.tenantId(), req.supplierId(), req.ingredientId());
        return catalogItemRepository.save(item);
    }

    public List<SupplierCatalogItemEntity> getCatalogItems(String tenantId, String supplierId) {
        return catalogItemRepository.findByTenantIdAndSupplierId(tenantId, supplierId);
    }
}
