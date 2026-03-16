package com.breadcost.api;

import com.breadcost.subscription.SubscriptionService;
import com.breadcost.subscription.SubscriptionTierEntity;
import com.breadcost.subscription.TenantSubscriptionEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Subscription REST controller.
 * BC-1701 – Subscription tier assignment by super-admin
 * BC-1702 – Feature access check by tenant
 */
@Tag(name = "Subscriptions", description = "Subscription tier management and feature access")
@RestController
@RequestMapping("/v2/subscriptions")
@PreAuthorize("hasAnyRole('Admin')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // ── BC-1701: Tier Management ──────────────────────────────────────────────

    /** GET /v2/subscriptions/tiers — list all tiers */
    @GetMapping("/tiers")
    public List<SubscriptionTierEntity> listTiers() {
        return subscriptionService.listTiers();
    }

    /** GET /v2/subscriptions/tiers/{level} */
    @GetMapping("/tiers/{level}")
    public SubscriptionTierEntity getTier(@PathVariable String level) {
        return subscriptionService.getTier(level);
    }

    /** PUT /v2/subscriptions/tenants/{tenantId} — assign tier */
    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantSubscriptionEntity> assignTier(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> body) {

        String tier = (String) body.get("tierLevel");
        String assignedBy = (String) body.getOrDefault("assignedBy", "admin");
        String startDateStr = (String) body.get("startDate");
        String expiryDateStr = (String) body.get("expiryDate");

        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
        LocalDate expiryDate = expiryDateStr != null ? LocalDate.parse(expiryDateStr) : null;

        TenantSubscriptionEntity sub = subscriptionService.assignTier(
                tenantId, tier, assignedBy, startDate, expiryDate);
        return ResponseEntity.ok(sub);
    }

    /** GET /v2/subscriptions/tenants/{tenantId} — current subscription */
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<Object> getSubscription(@PathVariable String tenantId) {
        Optional<TenantSubscriptionEntity> sub = subscriptionService.getActiveSub(tenantId);
        return sub.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── BC-1702: Feature Access ───────────────────────────────────────────────

    /** GET /v2/subscriptions/tenants/{tenantId}/features/{featureKey} — check feature access */
    @GetMapping("/tenants/{tenantId}/features/{featureKey}")
    public Map<String, Object> checkFeature(
            @PathVariable String tenantId,
            @PathVariable String featureKey) {
        boolean allowed = subscriptionService.hasFeature(tenantId, featureKey);
        return Map.of(
                "tenantId", tenantId,
                "featureKey", featureKey.toUpperCase(),
                "allowed", allowed
        );
    }

    /** GET /v2/subscriptions/tenants/{tenantId}/features — all enabled features */
    @GetMapping("/tenants/{tenantId}/features")
    public Map<String, Object> getFeatureAccess(@PathVariable String tenantId) {
        return subscriptionService.getFeatureAccess(tenantId);
    }

    // ── G-5: Subscription expiry management ──────────────────────────────────

    /** POST /v2/subscriptions/deactivate-expired — deactivate all expired subscriptions */
    @PostMapping("/deactivate-expired")
    public Map<String, Object> deactivateExpired() {
        int count = subscriptionService.deactivateExpired();
        return Map.of("deactivated", count);
    }

    /** GET /v2/subscriptions/expiring-soon?withinDays=7 — find subscriptions expiring soon */
    @GetMapping("/expiring-soon")
    public List<TenantSubscriptionEntity> expiringSoon(
            @RequestParam(defaultValue = "7") int withinDays) {
        return subscriptionService.findExpiringSoon(withinDays);
    }
}
