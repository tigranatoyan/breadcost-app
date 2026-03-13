package com.breadcost.unit.service;

import com.breadcost.supplier.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock private SupplierRepository supplierRepo;
    @Mock private SupplierCatalogItemRepository catalogRepo;
    @InjectMocks private SupplierService svc;

    // ── createSupplier ───────────────────────────────────────────────────────

    @Test
    void createSupplier_success() {
        when(supplierRepo.findByTenantIdAndName("t1", "Baker")).thenReturn(Optional.empty());
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var supplier = svc.createSupplier("t1", "Baker", "a@b.com", "123", "notes");

        assertEquals("t1", supplier.getTenantId());
        assertEquals("Baker", supplier.getName());
        assertEquals("a@b.com", supplier.getContactEmail());
    }

    @Test
    void createSupplier_duplicateName_throws() {
        when(supplierRepo.findByTenantIdAndName("t1", "Baker"))
                .thenReturn(Optional.of(SupplierEntity.builder().build()));

        assertThrows(IllegalArgumentException.class,
                () -> svc.createSupplier("t1", "Baker", null, null, null));
    }

    // ── updateSupplier ───────────────────────────────────────────────────────

    @Test
    void updateSupplier_updatesOnlyProvidedFields() {
        var supplier = SupplierEntity.builder()
                .supplierId("s1").tenantId("t1").name("Old").contactEmail("old@x.com").build();
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "s1")).thenReturn(Optional.of(supplier));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateSupplier("t1", "s1", "New Name", null, null, null);

        assertEquals("New Name", result.getName());
        assertEquals("old@x.com", result.getContactEmail()); // unchanged
    }

    @Test
    void updateSupplier_notFound_throws() {
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.updateSupplier("t1", "bad", "X", null, null, null));
    }

    // ── deleteSupplier ───────────────────────────────────────────────────────

    @Test
    void deleteSupplier_found_deletes() {
        var supplier = SupplierEntity.builder().supplierId("s1").tenantId("t1").build();
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "s1")).thenReturn(Optional.of(supplier));

        svc.deleteSupplier("t1", "s1");

        verify(supplierRepo).delete(supplier);
    }

    @Test
    void deleteSupplier_notFound_throws() {
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.deleteSupplier("t1", "bad"));
    }

    // ── addCatalogItem ───────────────────────────────────────────────────────

    @Test
    void addCatalogItem_setsDefaults() {
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.of(SupplierEntity.builder().supplierId("s1").tenantId("t1").build()));
        when(catalogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var item = svc.addCatalogItem("t1", "s1", "flour", "Flour",
                null, null, 0, 0, "KG");

        assertEquals(BigDecimal.ZERO, item.getUnitPrice());
        assertEquals("USD", item.getCurrency());
        assertEquals(1, item.getLeadTimeDays());
        assertEquals(1.0, item.getMoq());
    }

    @Test
    void addCatalogItem_withValues() {
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.of(SupplierEntity.builder().supplierId("s1").tenantId("t1").build()));
        when(catalogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var item = svc.addCatalogItem("t1", "s1", "flour", "Flour",
                new BigDecimal("2.50"), "AMD", 3, 50, "KG");

        assertEquals(new BigDecimal("2.50"), item.getUnitPrice());
        assertEquals("AMD", item.getCurrency());
        assertEquals(3, item.getLeadTimeDays());
        assertEquals(50.0, item.getMoq());
    }

    // ── getSupplier ──────────────────────────────────────────────────────────

    @Test
    void getSupplier_notFound_throws() {
        when(supplierRepo.findByTenantIdAndSupplierId("t1", "bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.getSupplier("t1", "bad"));
    }
}
