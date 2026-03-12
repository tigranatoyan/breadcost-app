package com.breadcost.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Subscription service.
 * BC-E17: Super-admin tier assignment and feature access enforcement.
 */
@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionTierRepository tierRepo;
    private final TenantSubscriptionRepository tenantSubRepo;

    public SubscriptionService(SubscriptionTierRepository tierRepo,
                                TenantSubscriptionRepository tenantSubRepo) {
        this.tierRepo = tierRepo;
        this.tenantSubRepo = tenantSubRepo;
    }

    // ── Tier Catalog ──────────────────────────────────────────────────────────

    /**
     * Seed default subscription tiers if none exist.
     */
    public void seedTiers() {
        if (tierRepo.count() > 0) return;
        tierRepo.saveAll(List.of(
                SubscriptionTierEntity.builder()
                        .tierId(UUID.randomUUID().toString())
                        .level(SubscriptionTierEntity.TierLevel.BASIC)
                        .name("Basic")
                        .description("Core POS and order management")
                        .enabledFeatures("ORDERS,POS,INVENTORY,CUSTOMERS")
                        .maxUsers(5)
                        .maxProducts(100)
                        .build(),
                SubscriptionTierEntity.builder()
                        .tierId(UUID.randomUUID().toString())
                        .level(SubscriptionTierEntity.TierLevel.STANDARD)
                        .name("Standard")
                        .description("Full bakery operations suite")
                        .enabledFeatures("ORDERS,POS,INVENTORY,CUSTOMERS,DELIVERY,INVOICING,LOYALTY,REPORTS,PRODUCTION")
                        .maxUsers(20)
                        .maxProducts(500)
                        .build(),
                SubscriptionTierEntity.builder()
                        .tierId(UUID.randomUUID().toString())
                        .level(SubscriptionTierEntity.TierLevel.ENTERPRISE)
                        .name("Enterprise")
                        .description("Full suite including AI features and unlimited capacity")
                        .enabledFeatures("ORDERS,POS,INVENTORY,CUSTOMERS,DELIVERY,INVOICING,LOYALTY,REPORTS,PRODUCTION,AI_BOT,WHATSAPP,SUPPLIER,SUBSCRIPTIONS")
                        .maxUsers(0)
                        .maxProducts(0)
                        .build()
        ));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionTierEntity> listTiers() {
        return tierRepo.findAll();
    }

    @Transactional(readOnly = true)
    public SubscriptionTierEntity getTier(String tierLevel) {
        return tierRepo.findByLevel(SubscriptionTierEntity.TierLevel.valueOf(tierLevel.toUpperCase()))
                .orElseThrow(() -> new NoSuchElementException("Tier not found: " + tierLevel));
    }

    // ── BC-1701: Tenant tier assignment ────────────────────────────────────────

    /**
     * Assign (or update) a subscription tier for a tenant.
     * BC-1701: Super-admin action.
     */
    public TenantSubscriptionEntity assignTier(String tenantId, String tierLevel,
                                                String assignedBy,
                                                LocalDate startDate, LocalDate expiryDate) {
        // Deactivate existing active subscription if any
        tenantSubRepo.findByTenantIdAndActive(tenantId, true).ifPresent(existing -> {
            existing.setActive(false);
            tenantSubRepo.save(existing);
        });

        SubscriptionTierEntity.TierLevel level =
                SubscriptionTierEntity.TierLevel.valueOf(tierLevel.toUpperCase());

        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .tierLevel(level)
                .startDate(startDate != null ? startDate : LocalDate.now())
                .expiryDate(expiryDate)
                .active(true)
                .assignedBy(assignedBy)
                .build();
        return tenantSubRepo.save(sub);
    }

    @Transactional(readOnly = true)
    public Optional<TenantSubscriptionEntity> getActiveSub(String tenantId) {
        return tenantSubRepo.findByTenantIdAndActive(tenantId, true);
    }

    // ── BC-1702: Feature access enforcement ────────────────────────────────────

    /**
     * Check if a tenant has access to a specific feature key.
     * BC-1702: Feature access enforcement by subscription tier.
     */
    @Transactional(readOnly = true)
    public boolean hasFeature(String tenantId, String featureKey) {
        Optional<TenantSubscriptionEntity> sub = tenantSubRepo.findByTenantIdAndActive(tenantId, true);
        if (sub.isEmpty()) return false;

        // Check expiry
        TenantSubscriptionEntity s = sub.get();
        if (s.getExpiryDate() != null && LocalDate.now().isAfter(s.getExpiryDate())) {
            return false;
        }

        Optional<SubscriptionTierEntity> tier = tierRepo.findByLevel(s.getTierLevel());
        return tier.map(t -> t.featureList().contains(featureKey.toUpperCase()))
                .orElse(false);
    }

    /**
     * Get all enabled features for a tenant.
     * BC-1702
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeatureAccess(String tenantId) {
        Optional<TenantSubscriptionEntity> sub = tenantSubRepo.findByTenantIdAndActive(tenantId, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        if (sub.isEmpty()) {
            result.put("tier", "NONE");
            result.put("features", List.of());
            result.put("maxUsers", 0);
            result.put("maxProducts", 0);
            return result;
        }
        TenantSubscriptionEntity s = sub.get();
        result.put("tier", s.getTierLevel().name());
        result.put("subscriptionId", s.getSubscriptionId());
        result.put("expiryDate", s.getExpiryDate());

        Optional<SubscriptionTierEntity> tier = tierRepo.findByLevel(s.getTierLevel());
        result.put("features", tier.map(SubscriptionTierEntity::featureList).orElse(List.of()));
        result.put("maxUsers", tier.map(SubscriptionTierEntity::getMaxUsers).orElse(0));
        result.put("maxProducts", tier.map(SubscriptionTierEntity::getMaxProducts).orElse(0));
        return result;
    }

    /**
     * Get the max users limit for a tenant (0 = unlimited).
     * BC-3102
     */
    @Transactional(readOnly = true)
    public int getMaxUsers(String tenantId) {
        return tenantSubRepo.findByTenantIdAndActive(tenantId, true)
                .flatMap(sub -> tierRepo.findByLevel(sub.getTierLevel()))
                .map(SubscriptionTierEntity::getMaxUsers)
                .orElse(0);
    }

    /**
     * Get the max products limit for a tenant (0 = unlimited).
     * BC-3102
     */
    @Transactional(readOnly = true)
    public int getMaxProducts(String tenantId) {
        return tenantSubRepo.findByTenantIdAndActive(tenantId, true)
                .flatMap(sub -> tierRepo.findByLevel(sub.getTierLevel()))
                .map(SubscriptionTierEntity::getMaxProducts)
                .orElse(0);
    }
}
