package com.breadcost.api;

import com.breadcost.loyalty.LoyaltyAccountEntity;
import com.breadcost.loyalty.LoyaltyService;
import com.breadcost.loyalty.LoyaltyTierEntity;
import com.breadcost.loyalty.LoyaltyTransactionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import com.breadcost.subscription.SubscriptionRequired;

/**
 * Customer Portal — Loyalty API  /v2/loyalty
 *
 * BC-1201: POST /v2/loyalty/award              — award points for purchase
 * BC-1202: POST /v2/loyalty/tiers              — create/update tier
 *          GET  /v2/loyalty/tiers?tenantId=... — list tiers
 * BC-1203: DELETE /v2/loyalty/tiers/{id}       — delete tier
 * BC-1204: POST /v2/loyalty/redeem             — redeem points
 * BC-1205: GET  /v2/loyalty/balance            — points balance
 *          GET  /v2/loyalty/history            — transaction history
 * BC-1206: covered by tiers CRUD above
 */
@RestController
@RequestMapping("/v2/loyalty")
@RequiredArgsConstructor
@Slf4j
@SubscriptionRequired("LOYALTY")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class AwardPointsRequest {
        @NotBlank private String tenantId;
        @NotBlank private String customerId;
        @NotBlank private String orderId;
        private BigDecimal orderTotal = BigDecimal.ZERO;
    }

    @Data
    public static class RedeemPointsRequest {
        @NotBlank private String tenantId;
        @NotBlank private String customerId;
        @Min(1) private long points;
        @NotBlank private String orderId;
    }

    @Data
    public static class SaveTierRequest {
        @NotBlank private String tenantId;
        @NotBlank private String name;
        @Min(0) private long minPoints;
        private double discountPct = 0.0;
        private double pointsPerDollar = 1.0;
        private String benefitsDescription;
    }

    // ── BC-1201: Award points ─────────────────────────────────────────────────

    @PostMapping("/award")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<LoyaltyAccountEntity> award(@Valid @RequestBody AwardPointsRequest req) {
        LoyaltyAccountEntity account = loyaltyService.awardPoints(
                req.getTenantId(), req.getCustomerId(),
                req.getOrderId(), req.getOrderTotal());
        return ResponseEntity.ok(account);
    }

    // ── BC-1204: Redeem points ────────────────────────────────────────────────

    @PostMapping("/redeem")
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<LoyaltyAccountEntity> redeem(@Valid @RequestBody RedeemPointsRequest req) {
        LoyaltyAccountEntity account = loyaltyService.redeemPoints(
                req.getTenantId(), req.getCustomerId(),
                req.getPoints(), req.getOrderId());
        return ResponseEntity.ok(account);
    }

    // ── BC-1205: Balance & history ────────────────────────────────────────────

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<LoyaltyAccountEntity> balance(
            @RequestParam String tenantId,
            @RequestParam String customerId) {
        return ResponseEntity.ok(loyaltyService.getBalance(tenantId, customerId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('Customer','Admin','Manager')")
    public ResponseEntity<List<LoyaltyTransactionEntity>> history(
            @RequestParam String tenantId,
            @RequestParam String customerId) {
        return ResponseEntity.ok(loyaltyService.getHistory(tenantId, customerId));
    }

    // ── BC-1202/1203/1206: Tier management ────────────────────────────────────

    @PostMapping("/tiers")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<LoyaltyTierEntity> saveTier(@Valid @RequestBody SaveTierRequest req) {
        LoyaltyTierEntity tier = loyaltyService.saveTier(
                req.getTenantId(), req.getName(),
                req.getMinPoints(), req.getDiscountPct(),
                req.getPointsPerDollar(), req.getBenefitsDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(tier);
    }

    @GetMapping("/tiers")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<List<LoyaltyTierEntity>> listTiers(@RequestParam String tenantId) {
        return ResponseEntity.ok(loyaltyService.listTiers(tenantId));
    }

    @DeleteMapping("/tiers/{id}")
    @PreAuthorize("hasAnyRole('Admin','Manager')")
    public ResponseEntity<Void> deleteTier(@PathVariable("id") String tierId) {
        loyaltyService.deleteTier(tierId);
        return ResponseEntity.noContent().build();
    }
}
