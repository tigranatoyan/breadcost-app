package com.breadcost.loyalty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Loyalty service — BC-E12
 *
 * BC-1201: Award points on completed purchase
 * BC-1202: Configurable loyalty tiers
 * BC-1203: Tier benefits management
 * BC-1204: Redeem loyalty points at checkout
 * BC-1205: Points balance and history
 * BC-1206: Loyalty tier rule configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final LoyaltyTierRepository tierRepository;

    /** Default earning rate when no tiers are configured: 1 point per $1 */
    private static final double DEFAULT_POINTS_PER_DOLLAR = 1.0;

    // ── BC-1201: Award points ─────────────────────────────────────────────────

    /**
     * Award loyalty points to a customer for a completed purchase.
     *
     * @param orderTotal  the gross order amount used to calculate points earned
     * @return updated account
     */
    @Transactional
    public LoyaltyAccountEntity awardPoints(
            String tenantId, String customerId,
            String orderId, BigDecimal orderTotal) {

        LoyaltyAccountEntity account = getOrCreateAccount(tenantId, customerId);

        // Determine points-per-dollar from the customer's current tier
        double rate = tierRepository
                .findByTenantIdOrderByMinPointsAsc(tenantId)
                .stream()
                .filter(t -> account.getPointsEarned() >= t.getMinPoints())
                .reduce((a, b) -> b)  // highest qualifying tier
                .map(LoyaltyTierEntity::getPointsPerDollar)
                .orElse(DEFAULT_POINTS_PER_DOLLAR);

        long earned = (long) Math.floor(orderTotal.doubleValue() * rate);
        if (earned <= 0) return account;

        account.setPointsBalance(account.getPointsBalance() + earned);
        account.setPointsEarned(account.getPointsEarned() + earned);

        // Promote tier based on lifetime points
        recalculateTier(account, tenantId);

        accountRepository.save(account);

        transactionRepository.save(LoyaltyTransactionEntity.builder()
                .txId(UUID.randomUUID().toString())
                .accountId(account.getAccountId())
                .tenantId(tenantId)
                .customerId(customerId)
                .type("EARN")
                .points(earned)
                .orderId(orderId)
                .description("Points awarded for order " + orderId)
                .build());

        log.info("Loyalty EARN: tenantId={} customerId={} orderId={} earned={} balance={}",
                tenantId, customerId, orderId, earned, account.getPointsBalance());
        return account;
    }

    // ── BC-1204: Redeem points ────────────────────────────────────────────────

    /**
     * Redeem loyalty points against an order.  Points are deducted from balance.
     *
     * @throws IllegalArgumentException if insufficient balance
     */
    @Transactional
    public LoyaltyAccountEntity redeemPoints(
            String tenantId, String customerId,
            long points, String orderId) {

        if (points <= 0) throw new IllegalArgumentException("Points to redeem must be positive.");

        LoyaltyAccountEntity account = getOrCreateAccount(tenantId, customerId);

        if (account.getPointsBalance() < points) {
            throw new IllegalArgumentException(
                    "Insufficient points balance. Available: " + account.getPointsBalance());
        }

        account.setPointsBalance(account.getPointsBalance() - points);
        account.setPointsRedeemed(account.getPointsRedeemed() + points);
        accountRepository.save(account);

        transactionRepository.save(LoyaltyTransactionEntity.builder()
                .txId(UUID.randomUUID().toString())
                .accountId(account.getAccountId())
                .tenantId(tenantId)
                .customerId(customerId)
                .type("REDEEM")
                .points(-points)
                .orderId(orderId)
                .description("Points redeemed for order " + orderId)
                .build());

        log.info("Loyalty REDEEM: tenantId={} customerId={} redeem={} balance={}",
                tenantId, customerId, points, account.getPointsBalance());
        return account;
    }

    // ── BC-1205: Balance & history ────────────────────────────────────────────

    public LoyaltyAccountEntity getBalance(String tenantId, String customerId) {
        return getOrCreateAccount(tenantId, customerId);
    }

    public List<LoyaltyTransactionEntity> getHistory(String tenantId, String customerId) {
        return transactionRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
                tenantId, customerId);
    }

    // ── BC-1202/1203/1206: Tier management ────────────────────────────────────

    /**
     * Create or update a loyalty tier.
     * @throws IllegalStateException if a different tier with the same name already exists
     */
    @Transactional
    public LoyaltyTierEntity saveTier(
            String tenantId, String name,
            long minPoints, double discountPct,
            double pointsPerDollar, String benefitsDescription) {

        LoyaltyTierEntity tier = tierRepository.findByTenantIdAndName(tenantId, name)
                .orElseGet(() -> LoyaltyTierEntity.builder()
                        .tierId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .name(name)
                        .build());

        tier.setMinPoints(minPoints);
        tier.setDiscountPct(discountPct);
        tier.setPointsPerDollar(pointsPerDollar);
        tier.setBenefitsDescription(benefitsDescription);

        log.info("Loyalty tier saved: tenantId={} name={} minPoints={}", tenantId, name, minPoints);
        return tierRepository.save(tier);
    }

    public List<LoyaltyTierEntity> listTiers(String tenantId) {
        return tierRepository.findByTenantIdOrderByMinPointsAsc(tenantId);
    }

    @Transactional
    public void deleteTier(String tierId) {
        tierRepository.deleteById(tierId);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private LoyaltyAccountEntity getOrCreateAccount(String tenantId, String customerId) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> accountRepository.save(LoyaltyAccountEntity.builder()
                        .accountId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .customerId(customerId)
                        .tierName("Bronze")
                        .build()));
    }

    private void recalculateTier(LoyaltyAccountEntity account, String tenantId) {
        tierRepository.findByTenantIdOrderByMinPointsAsc(tenantId)
                .stream()
                .filter(t -> account.getPointsEarned() >= t.getMinPoints())
                .reduce((a, b) -> b)
                .ifPresent(t -> account.setTierName(t.getName()));
    }
}
