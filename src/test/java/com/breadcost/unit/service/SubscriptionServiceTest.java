package com.breadcost.unit.service;

import com.breadcost.subscription.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionTierRepository tierRepo;
    @Mock private TenantSubscriptionRepository tenantSubRepo;

    @InjectMocks private SubscriptionService subscriptionService;

    private SubscriptionTierEntity enterpriseTier;
    private SubscriptionTierEntity basicTier;

    @BeforeEach
    void setUp() {
        enterpriseTier = SubscriptionTierEntity.builder()
                .tierId("tier-ent").level(SubscriptionTierEntity.TierLevel.ENTERPRISE)
                .name("Enterprise")
                .enabledFeatures("ORDERS,POS,INVENTORY,CUSTOMERS,DELIVERY,INVOICING,LOYALTY,REPORTS,PRODUCTION,AI_BOT")
                .maxUsers(0).maxProducts(0).build();

        basicTier = SubscriptionTierEntity.builder()
                .tierId("tier-basic").level(SubscriptionTierEntity.TierLevel.BASIC)
                .name("Basic")
                .enabledFeatures("ORDERS,POS,INVENTORY,CUSTOMERS")
                .maxUsers(5).maxProducts(100).build();
    }

    // ── hasFeature tests ─────────────────────────────────────────────────

    @Test
    void hasFeature_activeSub_returnsTrue() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-1").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.ENTERPRISE)
                .active(true).expiryDate(null).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));
        when(tierRepo.findByLevel(SubscriptionTierEntity.TierLevel.ENTERPRISE)).thenReturn(Optional.of(enterpriseTier));

        assertTrue(subscriptionService.hasFeature("t1", "ORDERS"));
        assertTrue(subscriptionService.hasFeature("t1", "AI_BOT"));
    }

    @Test
    void hasFeature_basicTier_lacksAdvancedFeatures() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-2").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.BASIC)
                .active(true).expiryDate(null).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));
        when(tierRepo.findByLevel(SubscriptionTierEntity.TierLevel.BASIC)).thenReturn(Optional.of(basicTier));

        assertTrue(subscriptionService.hasFeature("t1", "ORDERS"));
        assertFalse(subscriptionService.hasFeature("t1", "AI_BOT"));
        assertFalse(subscriptionService.hasFeature("t1", "DELIVERY"));
    }

    @Test
    void hasFeature_expiredSub_returnsFalse() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-3").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.ENTERPRISE)
                .active(true).expiryDate(LocalDate.now().minusDays(1)).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));

        assertFalse(subscriptionService.hasFeature("t1", "ORDERS"));
    }

    @Test
    void hasFeature_noActiveSub_returnsFalse() {
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.empty());

        assertFalse(subscriptionService.hasFeature("t1", "ORDERS"));
    }

    // ── getFeatureAccess tests ───────────────────────────────────────────

    @Test
    void getFeatureAccess_activeSub_returnsFeatures() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-1").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.BASIC)
                .active(true).expiryDate(null).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));
        when(tierRepo.findByLevel(SubscriptionTierEntity.TierLevel.BASIC)).thenReturn(Optional.of(basicTier));

        Map<String, Object> result = subscriptionService.getFeatureAccess("t1");

        assertEquals("BASIC", result.get("tier"));
        assertEquals(false, result.get("expired"));
        assertEquals(5, result.get("maxUsers"));
        assertEquals(100, result.get("maxProducts"));
    }

    @Test
    void getFeatureAccess_expired_returnsEmptyFeaturesAndZeroLimits() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-exp").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.ENTERPRISE)
                .active(true).expiryDate(LocalDate.now().minusDays(5)).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));

        Map<String, Object> result = subscriptionService.getFeatureAccess("t1");

        assertEquals(true, result.get("expired"));
        assertEquals(List.of(), result.get("features"));
        assertEquals(0, result.get("maxUsers"));
        assertEquals(0, result.get("maxProducts"));
    }

    @Test
    void getFeatureAccess_noSub_returnsNoneTier() {
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.empty());

        Map<String, Object> result = subscriptionService.getFeatureAccess("t1");

        assertEquals("NONE", result.get("tier"));
        assertEquals(false, result.get("expired"));
    }

    // ── getMaxUsers / getMaxProducts with expiry ─────────────────────────

    @Test
    void getMaxUsers_expired_returnsZero() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-exp").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.BASIC)
                .active(true).expiryDate(LocalDate.now().minusDays(1)).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));

        assertEquals(0, subscriptionService.getMaxUsers("t1"));
    }

    @Test
    void getMaxProducts_active_returnsLimit() {
        TenantSubscriptionEntity sub = TenantSubscriptionEntity.builder()
                .subscriptionId("sub-1").tenantId("t1")
                .tierLevel(SubscriptionTierEntity.TierLevel.BASIC)
                .active(true).expiryDate(LocalDate.now().plusDays(30)).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(sub));
        when(tierRepo.findByLevel(SubscriptionTierEntity.TierLevel.BASIC)).thenReturn(Optional.of(basicTier));

        assertEquals(100, subscriptionService.getMaxProducts("t1"));
    }

    // ── deactivateExpired tests ──────────────────────────────────────────

    @Test
    void deactivateExpired_deactivatesExpiredSubs() {
        TenantSubscriptionEntity expired1 = TenantSubscriptionEntity.builder()
                .subscriptionId("exp-1").tenantId("t1").active(true)
                .expiryDate(LocalDate.now().minusDays(3)).build();
        TenantSubscriptionEntity expired2 = TenantSubscriptionEntity.builder()
                .subscriptionId("exp-2").tenantId("t2").active(true)
                .expiryDate(LocalDate.now().minusDays(1)).build();
        TenantSubscriptionEntity active = TenantSubscriptionEntity.builder()
                .subscriptionId("act-1").tenantId("t3").active(true)
                .expiryDate(LocalDate.now().plusDays(30)).build();
        when(tenantSubRepo.findAll()).thenReturn(List.of(expired1, expired2, active));

        int count = subscriptionService.deactivateExpired();

        assertEquals(2, count);
        verify(tenantSubRepo, times(2)).save(argThat(sub -> !sub.isActive()));
    }

    @Test
    void deactivateExpired_noExpiryDate_skipped() {
        TenantSubscriptionEntity noExpiry = TenantSubscriptionEntity.builder()
                .subscriptionId("no-exp").tenantId("t1").active(true)
                .expiryDate(null).build();
        when(tenantSubRepo.findAll()).thenReturn(List.of(noExpiry));

        assertEquals(0, subscriptionService.deactivateExpired());
    }

    // ── findExpiringSoon tests ────────────────────────────────────────────

    @Test
    void findExpiringSoon_returnsSubsWithinWindow() {
        TenantSubscriptionEntity soon = TenantSubscriptionEntity.builder()
                .subscriptionId("soon-1").tenantId("t1").active(true)
                .expiryDate(LocalDate.now().plusDays(5)).build();
        TenantSubscriptionEntity later = TenantSubscriptionEntity.builder()
                .subscriptionId("later-1").tenantId("t2").active(true)
                .expiryDate(LocalDate.now().plusDays(60)).build();
        TenantSubscriptionEntity expired = TenantSubscriptionEntity.builder()
                .subscriptionId("exp-1").tenantId("t3").active(true)
                .expiryDate(LocalDate.now().minusDays(1)).build();
        when(tenantSubRepo.findAll()).thenReturn(List.of(soon, later, expired));

        List<TenantSubscriptionEntity> result = subscriptionService.findExpiringSoon(30);

        assertEquals(1, result.size());
        assertEquals("soon-1", result.get(0).getSubscriptionId());
    }

    // ── assignTier tests ─────────────────────────────────────────────────

    @Test
    void assignTier_deactivatesExistingAndCreatesNew() {
        TenantSubscriptionEntity existing = TenantSubscriptionEntity.builder()
                .subscriptionId("old-sub").tenantId("t1").active(true)
                .tierLevel(SubscriptionTierEntity.TierLevel.BASIC).build();
        when(tenantSubRepo.findByTenantIdAndActive("t1", true)).thenReturn(Optional.of(existing));
        when(tenantSubRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TenantSubscriptionEntity result = subscriptionService.assignTier(
                "t1", "ENTERPRISE", "admin", LocalDate.now(), LocalDate.now().plusYears(1));

        assertFalse(existing.isActive());
        assertEquals(SubscriptionTierEntity.TierLevel.ENTERPRISE, result.getTierLevel());
        assertTrue(result.isActive());
        verify(tenantSubRepo, times(2)).save(any());
    }
}
