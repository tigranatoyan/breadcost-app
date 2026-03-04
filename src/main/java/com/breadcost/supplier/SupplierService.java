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
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

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
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    @Transactional
    public void deleteSupplier(String tenantId, String supplierId) {
        SupplierEntity supplier = supplierRepository.findByTenantIdAndSupplierId(tenantId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        supplierRepository.delete(supplier);
    }

    // ── BC-1301: Catalog items ────────────────────────────────────────────────

    @Transactional
    public SupplierCatalogItemEntity addCatalogItem(String tenantId, String supplierId,
                                                     String ingredientId, String ingredientName,
                                                     BigDecimal unitPrice, String currency,
                                                     int leadTimeDays, double moq, String unit) {
        // Verify supplier exists
        getSupplier(tenantId, supplierId);

        SupplierCatalogItemEntity item = SupplierCatalogItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .supplierId(supplierId)
                .ingredientId(ingredientId)
                .ingredientName(ingredientName)
                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                .currency(currency != null ? currency : "USD")
                .leadTimeDays(leadTimeDays > 0 ? leadTimeDays : 1)
                .moq(moq > 0 ? moq : 1.0)
                .unit(unit)
                .build();

        log.info("Adding catalog item: tenantId={} supplierId={} ingredient={}", tenantId, supplierId, ingredientId);
        return catalogItemRepository.save(item);
    }

    public List<SupplierCatalogItemEntity> getCatalogItems(String tenantId, String supplierId) {
        return catalogItemRepository.findByTenantIdAndSupplierId(tenantId, supplierId);
    }
}
