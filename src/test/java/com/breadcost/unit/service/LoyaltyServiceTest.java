package com.breadcost.unit.service;

import com.breadcost.loyalty.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock private LoyaltyAccountRepository accountRepo;
    @Mock private LoyaltyTransactionRepository txRepo;
    @Mock private LoyaltyTierRepository tierRepo;
    @InjectMocks private LoyaltyService svc;

    // ── awardPoints ──────────────────────────────────────────────────────────

    @Test
    void award_defaultRate_1pointPerDollar() {
        var account = account("t1", "c1", 0, 0, 0);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));
        when(tierRepo.findByTenantIdOrderByMinPointsAsc("t1")).thenReturn(List.of());
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.awardPoints("t1", "c1", "ord-1", new BigDecimal("50.75"));

        assertEquals(50L, result.getPointsBalance());
        assertEquals(50L, result.getPointsEarned());
    }

    @Test
    void award_tierRate_usesHighestQualifying() {
        var account = account("t1", "c1", 500, 500, 0);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));
        var bronze = tier("Bronze", 0, 1.0);
        var silver = tier("Silver", 200, 2.0);
        when(tierRepo.findByTenantIdOrderByMinPointsAsc("t1")).thenReturn(List.of(bronze, silver));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.awardPoints("t1", "c1", "ord-2", new BigDecimal("100"));

        // Silver tier: 2.0 points per dollar → 200 earned
        assertEquals(500L + 200L, result.getPointsBalance());
    }

    @Test
    void award_zeroOrderTotal_noPointsAdded() {
        var account = account("t1", "c1", 100, 100, 0);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));
        when(tierRepo.findByTenantIdOrderByMinPointsAsc("t1")).thenReturn(List.of());

        var result = svc.awardPoints("t1", "c1", "ord-0", BigDecimal.ZERO);

        assertEquals(100L, result.getPointsBalance());
        verify(accountRepo, never()).save(any());
    }

    @Test
    void award_autoCreatesAccountIfMissing() {
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c99")).thenReturn(Optional.empty());
        var newAccount = account("t1", "c99", 0, 0, 0);
        when(accountRepo.save(any())).thenReturn(newAccount).thenAnswer(inv -> inv.getArgument(0));
        when(tierRepo.findByTenantIdOrderByMinPointsAsc("t1")).thenReturn(List.of());
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.awardPoints("t1", "c99", "ord-1", new BigDecimal("10"));

        assertNotNull(result);
    }

    // ── redeemPoints ─────────────────────────────────────────────────────────

    @Test
    void redeem_sufficientBalance_deducts() {
        var account = account("t1", "c1", 500, 800, 300);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.redeemPoints("t1", "c1", 200L, "ord-5");

        assertEquals(300L, result.getPointsBalance());
        assertEquals(500L, result.getPointsRedeemed());
    }

    @Test
    void redeem_insufficientBalance_throws() {
        var account = account("t1", "c1", 50, 50, 0);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.redeemPoints("t1", "c1", 100L, "ord-x"));
        assertTrue(ex.getMessage().contains("Insufficient"));
    }

    @Test
    void redeem_zeroPoints_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.redeemPoints("t1", "c1", 0L, "ord-x"));
    }

    @Test
    void redeem_negativePoints_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.redeemPoints("t1", "c1", -5L, "ord-x"));
    }

    // ── getBalance ───────────────────────────────────────────────────────────

    @Test
    void getBalance_existingAccount() {
        var account = account("t1", "c1", 100, 200, 100);
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(account));

        assertEquals(100L, svc.getBalance("t1", "c1").getPointsBalance());
    }

    @Test
    void getBalance_autoCreatesIfMissing() {
        when(accountRepo.findByTenantIdAndCustomerId("t1", "c99")).thenReturn(Optional.empty());
        var newAccount = account("t1", "c99", 0, 0, 0);
        when(accountRepo.save(any())).thenReturn(newAccount);

        var result = svc.getBalance("t1", "c99");

        assertEquals(0L, result.getPointsBalance());
        assertEquals("Bronze", result.getTierName());
    }

    // ── saveTier ─────────────────────────────────────────────────────────────

    @Test
    void saveTier_createsNew() {
        when(tierRepo.findByTenantIdAndName("t1", "Gold")).thenReturn(Optional.empty());
        when(tierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.saveTier("t1", "Gold", 1000L, 10.0, 3.0, "Free delivery");

        verify(tierRepo).save(argThat(t -> "Gold".equals(t.getName()) && t.getMinPoints() == 1000L));
    }

    @Test
    void saveTier_updatesExisting() {
        var existing = LoyaltyTierEntity.builder()
                .tierId("tid1").tenantId("t1").name("Silver").minPoints(200).build();
        when(tierRepo.findByTenantIdAndName("t1", "Silver")).thenReturn(Optional.of(existing));
        when(tierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.saveTier("t1", "Silver", 300L, 5.0, 2.0, "Updated benefits");

        assertEquals(300L, existing.getMinPoints());
        assertEquals(2.0, existing.getPointsPerDollar());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private LoyaltyAccountEntity account(String tenantId, String customerId,
                                          long balance, long earned, long redeemed) {
        return LoyaltyAccountEntity.builder()
                .accountId("acc-" + customerId)
                .tenantId(tenantId)
                .customerId(customerId)
                .pointsBalance(balance)
                .pointsEarned(earned)
                .pointsRedeemed(redeemed)
                .tierName("Bronze")
                .build();
    }

    private LoyaltyTierEntity tier(String name, long minPoints, double pointsPerDollar) {
        return LoyaltyTierEntity.builder()
                .tierId("tier-" + name)
                .name(name)
                .minPoints(minPoints)
                .pointsPerDollar(pointsPerDollar)
                .build();
    }
}
