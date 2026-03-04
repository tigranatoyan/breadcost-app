package com.breadcost.api;

import com.breadcost.masterdata.TenantConfigEntity;
import com.breadcost.masterdata.TenantConfigRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant configuration — FR-11.5
 * GET/PUT /v1/config
 */
@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final TenantConfigRepository configRepository;

    @Data
    public static class UpdateConfigRequest {
        private String displayName;
        private String orderCutoffTime;
        private Double rushOrderPremiumPct;
        private String mainCurrency;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','FinanceUser','Viewer')")
    public ResponseEntity<TenantConfigEntity> getConfig(@RequestParam String tenantId) {
        TenantConfigEntity cfg = configRepository.findById(tenantId)
                .orElseGet(() -> TenantConfigEntity.builder()
                        .tenantId(tenantId)
                        .build());
        return ResponseEntity.ok(cfg);
    }

    @PutMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<TenantConfigEntity> updateConfig(
            @RequestParam String tenantId,
            @RequestBody UpdateConfigRequest req) {

        TenantConfigEntity cfg = configRepository.findById(tenantId)
                .orElseGet(() -> TenantConfigEntity.builder().tenantId(tenantId).build());

        if (req.getDisplayName() != null)        cfg.setDisplayName(req.getDisplayName());
        if (req.getOrderCutoffTime() != null)     cfg.setOrderCutoffTime(req.getOrderCutoffTime());
        if (req.getRushOrderPremiumPct() != null) cfg.setRushOrderPremiumPct(req.getRushOrderPremiumPct());
        if (req.getMainCurrency() != null)        cfg.setMainCurrency(req.getMainCurrency());

        return ResponseEntity.ok(configRepository.save(cfg));
    }
}
