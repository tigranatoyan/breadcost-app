package com.breadcost.api;

import com.breadcost.supplier.SupplierApiConfigEntity;
import com.breadcost.supplier.SupplierApiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Supplier API integration endpoints — BC-2202 (FR-6.4)
 */
@RestController
@RequestMapping("/v3/supplier-api")
@RequiredArgsConstructor
public class SupplierApiController {

    private final SupplierApiService service;

    // ── Config CRUD ───────────────────────────────────────────────────────────

    @PostMapping("/configs")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierApiConfigEntity saveConfig(@RequestBody @Valid SaveConfigRequest req) {
        return service.saveConfig(req.tenantId, req.supplierId,
                req.apiUrl, req.apiKeyRef, req.format, req.enabled);
    }

    @GetMapping("/configs")
    public List<SupplierApiConfigEntity> listConfigs(@RequestParam String tenantId) {
        return service.listConfigs(tenantId);
    }

    @GetMapping("/configs/{supplierId}")
    public SupplierApiConfigEntity getConfig(@RequestParam String tenantId,
                                              @PathVariable String supplierId) {
        return service.getConfig(tenantId, supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No API config for supplier " + supplierId));
    }

    @Data
    static class SaveConfigRequest {
        @NotBlank String tenantId;
        @NotBlank String supplierId;
        @NotBlank String apiUrl;
        String apiKeyRef;
        String format;
        Boolean enabled;
    }

    // ── Send PO ───────────────────────────────────────────────────────────────

    @PostMapping("/send-po")
    public Map<String, Object> sendPurchaseOrder(@RequestBody @Valid SendPoRequest req) {
        boolean ok = service.sendPurchaseOrder(req.tenantId, req.poId);
        return Map.of("poId", req.poId, "sent", ok);
    }

    @Data
    static class SendPoRequest {
        @NotBlank String tenantId;
        @NotBlank String poId;
    }
}
