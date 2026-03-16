package com.breadcost.unit.service;

import com.breadcost.purchaseorder.*;
import com.breadcost.supplier.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierApiServiceTest {

    @Mock private SupplierApiConfigRepository configRepository;
    @Mock private PurchaseOrderRepository poRepository;
    @Mock private PurchaseOrderLineRepository poLineRepository;
    @Mock private RestClient.Builder restClientBuilder;
    @InjectMocks private SupplierApiService svc;

    // ── saveConfig ───────────────────────────────────────────────────────────

    @Test
    void saveConfig_newConfig_creates() {
        when(configRepository.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.empty());
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.saveConfig("t1", "s1", "https://api.example.com",
                "key-ref", "JSON", true);

        assertEquals("t1", result.getTenantId());
        assertEquals("s1", result.getSupplierId());
        assertEquals("https://api.example.com", result.getApiUrl());
        assertTrue(result.isEnabled());
    }

    @Test
    void saveConfig_existingConfig_updates() {
        var existing = SupplierApiConfigEntity.builder()
                .configId("c1").tenantId("t1").supplierId("s1")
                .apiUrl("https://old.example.com").enabled(false).build();
        when(configRepository.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.of(existing));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.saveConfig("t1", "s1", "https://new.example.com",
                "new-key", null, true);

        assertEquals("https://new.example.com", result.getApiUrl());
        assertEquals("new-key", result.getApiKeyRef());
        assertTrue(result.isEnabled());
    }

    // ── getConfig ────────────────────────────────────────────────────────────

    @Test
    void getConfig_returnsOptional() {
        var config = SupplierApiConfigEntity.builder()
                .configId("c1").tenantId("t1").supplierId("s1").build();
        when(configRepository.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.of(config));

        var result = svc.getConfig("t1", "s1");

        assertTrue(result.isPresent());
        assertEquals("c1", result.get().getConfigId());
    }

    // ── sendPurchaseOrder ────────────────────────────────────────────────────

    @Test
    void sendPurchaseOrder_notApproved_throws() {
        var po = PurchaseOrderEntity.builder()
                .poId("po1").tenantId("t1").supplierId("s1")
                .status(PurchaseOrderEntity.PoStatus.DRAFT).build();
        when(poRepository.findByTenantIdAndPoId("t1", "po1"))
                .thenReturn(Optional.of(po));

        assertThrows(IllegalStateException.class,
                () -> svc.sendPurchaseOrder("t1", "po1"));
    }

    @Test
    void sendPurchaseOrder_noConfig_throws() {
        var po = PurchaseOrderEntity.builder()
                .poId("po1").tenantId("t1").supplierId("s1")
                .status(PurchaseOrderEntity.PoStatus.APPROVED).build();
        when(poRepository.findByTenantIdAndPoId("t1", "po1"))
                .thenReturn(Optional.of(po));
        when(configRepository.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.sendPurchaseOrder("t1", "po1"));
    }

    @Test
    void sendPurchaseOrder_configDisabled_throws() {
        var po = PurchaseOrderEntity.builder()
                .poId("po1").tenantId("t1").supplierId("s1")
                .supplierName("Acme").status(PurchaseOrderEntity.PoStatus.APPROVED).build();
        when(poRepository.findByTenantIdAndPoId("t1", "po1"))
                .thenReturn(Optional.of(po));
        var config = SupplierApiConfigEntity.builder()
                .configId("c1").tenantId("t1").supplierId("s1").enabled(false).build();
        when(configRepository.findByTenantIdAndSupplierId("t1", "s1"))
                .thenReturn(Optional.of(config));

        assertThrows(IllegalStateException.class,
                () -> svc.sendPurchaseOrder("t1", "po1"));
    }

    @Test
    void sendPurchaseOrder_poNotFound_throws() {
        when(poRepository.findByTenantIdAndPoId("t1", "bad"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.sendPurchaseOrder("t1", "bad"));
    }
}
