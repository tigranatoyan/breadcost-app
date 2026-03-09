package com.breadcost.supplier;

import com.breadcost.purchaseorder.PurchaseOrderEntity;
import com.breadcost.purchaseorder.PurchaseOrderLineEntity;
import com.breadcost.purchaseorder.PurchaseOrderRepository;
import com.breadcost.purchaseorder.PurchaseOrderLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

/**
 * Supplier API service — BC-2202 (FR-6.4)
 *
 * Manages supplier API configs and sends approved POs electronically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierApiService {

    private final SupplierApiConfigRepository configRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final RestClient.Builder restClientBuilder;

    // ── Config CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public SupplierApiConfigEntity saveConfig(String tenantId, String supplierId,
                                               String apiUrl, String apiKeyRef,
                                               String format, boolean enabled) {
        Optional<SupplierApiConfigEntity> existing =
                configRepository.findByTenantIdAndSupplierId(tenantId, supplierId);

        SupplierApiConfigEntity config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setApiUrl(apiUrl);
            config.setApiKeyRef(apiKeyRef);
            if (format != null) config.setFormat(format);
            config.setEnabled(enabled);
        } else {
            config = SupplierApiConfigEntity.builder()
                    .configId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .supplierId(supplierId)
                    .apiUrl(apiUrl)
                    .apiKeyRef(apiKeyRef)
                    .format(format != null ? format : "JSON")
                    .enabled(enabled)
                    .build();
        }

        log.info("Saving supplier API config: tenantId={} supplierId={} enabled={}",
                tenantId, supplierId, enabled);
        return configRepository.save(config);
    }

    public Optional<SupplierApiConfigEntity> getConfig(String tenantId, String supplierId) {
        return configRepository.findByTenantIdAndSupplierId(tenantId, supplierId);
    }

    public List<SupplierApiConfigEntity> listConfigs(String tenantId) {
        return configRepository.findByTenantId(tenantId);
    }

    // ── Send PO via API ───────────────────────────────────────────────────────

    /**
     * Send an approved PO to the supplier's API endpoint.
     * Returns true if successfully sent, false otherwise.
     */
    @Transactional
    public boolean sendPurchaseOrder(String tenantId, String poId) {
        PurchaseOrderEntity po = poRepository.findByTenantIdAndPoId(tenantId, poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));

        if (po.getStatus() != PurchaseOrderEntity.PoStatus.APPROVED) {
            throw new IllegalStateException("PO must be APPROVED to send via API. Current: " + po.getStatus());
        }

        SupplierApiConfigEntity config = configRepository
                .findByTenantIdAndSupplierId(tenantId, po.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No API config for supplier: " + po.getSupplierId()));

        if (!config.isEnabled()) {
            throw new IllegalStateException("Supplier API is not enabled for: " + po.getSupplierName());
        }

        List<PurchaseOrderLineEntity> lines =
                poLineRepository.findByPoId(poId);

        // Build payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("poId", po.getPoId());
        payload.put("supplierName", po.getSupplierName());
        payload.put("totalAmount", po.getTotalAmount());
        payload.put("currency", po.getFxCurrencyCode());
        payload.put("notes", po.getNotes());

        List<Map<String, Object>> lineItems = new ArrayList<>();
        for (PurchaseOrderLineEntity line : lines) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ingredientId", line.getIngredientId());
            item.put("ingredientName", line.getIngredientName());
            item.put("qty", line.getQty());
            item.put("unit", line.getUnit());
            item.put("unitPrice", line.getUnitPrice());
            item.put("lineTotal", line.getLineTotal());
            lineItems.add(item);
        }
        payload.put("lines", lineItems);

        try {
            RestClient client = restClientBuilder.build();
            client.post()
                    .uri(config.getApiUrl())
                    .header("X-Api-Key", config.getApiKeyRef())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            config.setLastSentAt(Instant.now());
            configRepository.save(config);

            po.setStatus(PurchaseOrderEntity.PoStatus.APPROVED); // keep APPROVED; no SENT status defined
            poRepository.save(po);

            log.info("PO {} sent to supplier API: {}", poId, config.getApiUrl());
            return true;
        } catch (Exception e) {
            log.error("Failed to send PO {} to supplier API: {}", poId, e.getMessage());
            return false;
        }
    }
}
