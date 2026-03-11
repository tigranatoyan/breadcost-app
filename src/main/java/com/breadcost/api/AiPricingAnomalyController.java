package com.breadcost.api;

import com.breadcost.ai.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * AI pricing + anomaly endpoints — BC-2001 (FR-12.5), BC-2002 (FR-12.6)
 */
@RestController
@RequestMapping("/v3/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager','FinanceUser')")
public class AiPricingAnomalyController {

    private final AiPricingAnomalyService service;

    // ── BC-2001: Pricing Suggestions ─────────────────────────────────────────

    @PostMapping("/pricing/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiPricingSuggestionEntity> generatePricing(@RequestBody @Valid TenantRequest req) {
        return service.generatePricingSuggestions(req.tenantId);
    }

    @GetMapping("/pricing")
    public List<AiPricingSuggestionEntity> getPricing(@RequestParam String tenantId) {
        return service.getPricingSuggestions(tenantId);
    }

    @GetMapping("/pricing/pending")
    public List<AiPricingSuggestionEntity> getPendingPricing(@RequestParam String tenantId) {
        return service.getPendingPricingSuggestions(tenantId);
    }

    @PostMapping("/pricing/{suggestionId}/dismiss")
    public AiPricingSuggestionEntity dismissPricing(@PathVariable String suggestionId) {
        return service.dismissPricingSuggestion(suggestionId);
    }

    @PostMapping("/pricing/{suggestionId}/accept")
    public AiPricingSuggestionEntity acceptPricing(@PathVariable String suggestionId) {
        return service.acceptPricingSuggestion(suggestionId);
    }

    // ── BC-2002: Anomaly Alerts ──────────────────────────────────────────────

    @PostMapping("/anomalies/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiAnomalyAlertEntity> generateAlerts(@RequestBody @Valid TenantRequest req) {
        return service.generateAnomalyAlerts(req.tenantId);
    }

    @GetMapping("/anomalies")
    public List<AiAnomalyAlertEntity> getAlerts(@RequestParam String tenantId) {
        return service.getAlerts(tenantId);
    }

    @GetMapping("/anomalies/active")
    public List<AiAnomalyAlertEntity> getActiveAlerts(@RequestParam String tenantId) {
        return service.getActiveAlerts(tenantId);
    }

    @PostMapping("/anomalies/{alertId}/acknowledge")
    public AiAnomalyAlertEntity acknowledgeAlert(@PathVariable String alertId) {
        return service.acknowledgeAlert(alertId);
    }

    @PostMapping("/anomalies/{alertId}/dismiss")
    public AiAnomalyAlertEntity dismissAlert(@PathVariable String alertId) {
        return service.dismissAlert(alertId);
    }

    @Data
    static class TenantRequest {
        @NotBlank String tenantId;
    }
}
