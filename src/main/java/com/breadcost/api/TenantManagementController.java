package com.breadcost.api;

import com.breadcost.masterdata.TenantConfigEntity;
import com.breadcost.multitenancy.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * D4 — Tenant management endpoints: onboarding, branding, data export, cross-tenant admin.
 */
@Tag(name = "Tenant Management", description = "Multi-tenant administration and onboarding")
@RestController
@RequestMapping("/v3/tenants")
@RequiredArgsConstructor
public class TenantManagementController {

    private final TenantManagementService service;

    // ── D4.1: Onboarding ─────────────────────────────────────────────────────

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantOnboardingEntity submitOnboarding(@RequestBody @Valid OnboardingRequest req) {
        return service.submitOnboardingRequest(req.businessName, req.ownerEmail,
                req.ownerName, req.ownerPhone, req.country, req.currency, req.requestedTier);
    }

    @PostMapping("/onboarding/{requestId}/approve")
    @PreAuthorize("hasRole('Admin')")
    public TenantOnboardingEntity approveOnboarding(@PathVariable String requestId) {
        return service.approveOnboarding(requestId);
    }

    @PostMapping("/onboarding/{requestId}/reject")
    @PreAuthorize("hasRole('Admin')")
    public TenantOnboardingEntity rejectOnboarding(@PathVariable String requestId,
                                                    @RequestBody RejectRequest req) {
        return service.rejectOnboarding(requestId, req.reason);
    }

    @GetMapping("/onboarding/pending")
    @PreAuthorize("hasRole('Admin')")
    public List<TenantOnboardingEntity> getPendingRequests() {
        return service.getPendingRequests();
    }

    @GetMapping("/onboarding")
    @PreAuthorize("hasRole('Admin')")
    public List<TenantOnboardingEntity> getAllRequests() {
        return service.getAllRequests();
    }

    @Data
    static class OnboardingRequest {
        @NotBlank String businessName;
        @NotBlank @Email String ownerEmail;
        String ownerName;
        String ownerPhone;
        String country;
        String currency;
        String requestedTier;
    }

    @Data
    static class RejectRequest { String reason; }

    // ── D4.3: Branding ───────────────────────────────────────────────────────

    @GetMapping("/{tenantId}/branding")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public TenantBrandingEntity getBranding(@PathVariable String tenantId) {
        return service.getBranding(tenantId);
    }

    @PutMapping("/{tenantId}/branding")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public TenantBrandingEntity updateBranding(@PathVariable String tenantId,
                                                @RequestBody @Valid BrandingRequest req) {
        return service.updateBranding(
                new TenantManagementService.BrandingUpdateRequest(
                        tenantId, req.logoUrl, req.primaryColor,
                        req.secondaryColor, req.accentColor, req.receiptBusinessName,
                        req.receiptFooter, req.receiptHeader, req.locale, req.timezone));
    }

    @Data
    static class BrandingRequest {
        String logoUrl;
        String primaryColor;
        String secondaryColor;
        String accentColor;
        String receiptBusinessName;
        String receiptFooter;
        String receiptHeader;
        String locale;
        String timezone;
    }

    // ── D4.2: GDPR Data Export ───────────────────────────────────────────────

    @GetMapping("/{tenantId}/export")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> exportData(@PathVariable String tenantId) {
        return service.exportTenantData(tenantId);
    }

    // ── D4.4: Cross-Tenant Platform Admin ────────────────────────────────────

    @GetMapping("/platform/overview")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> getPlatformOverview() {
        return service.getPlatformOverview();
    }

    @PostMapping("/{tenantId}/suspend")
    @PreAuthorize("hasRole('Admin')")
    public TenantConfigEntity suspendTenant(@PathVariable String tenantId) {
        return service.suspendTenant(tenantId);
    }

    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasRole('Admin')")
    public TenantConfigEntity activateTenant(@PathVariable String tenantId) {
        return service.activateTenant(tenantId);
    }
}
