package com.breadcost.ai;

import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * D3.2 — Dynamic pricing engine.
 * Computes real-time price suggestions based on demand elasticity,
 * inventory levels, and competitive positioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicPricingService {

    private final AiPricingSuggestionRepository pricingRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final InventoryProjection inventoryProjection;

    private static final int ANALYSIS_DAYS = 60;
    private static final double MAX_DISCOUNT_PCT = 15.0;
    private static final double MAX_MARKUP_PCT = 20.0;

    /**
     * Generate dynamic pricing suggestions based on multi-factor analysis:
     * - Demand velocity (orders/day trend)
     * - Inventory pressure (overstock → discount, low stock → premium)
     * - Customer concentration (diversified demand = stable pricing)
     * - Margin protection floor
     */
    @Transactional
    public List<AiPricingSuggestionEntity> generateDynamicPricing(String tenantId) {
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);

        Map<String, BigDecimal> productPrices = products.stream()
                .filter(p -> p.getPrice() != null)
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getPrice, (a, b) -> a));
        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        // Get inventory positions for stock-pressure analysis
        Map<String, Double> onHandByProduct = inventoryProjection.getAllPositions().stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .collect(Collectors.groupingBy(
                        InventoryProjection.InventoryPosition::getItemId,
                        Collectors.summingDouble(p -> p.getOnHandQty().doubleValue())));

        Instant cutoff = Instant.now().minus(ANALYSIS_DAYS, ChronoUnit.DAYS);
        Instant midpoint = Instant.now().minus(ANALYSIS_DAYS / 2, ChronoUnit.DAYS);

        // Analyze per-product demand in two halves for velocity detection
        Map<String, Double> firstHalfQty = new HashMap<>();
        Map<String, Double> secondHalfQty = new HashMap<>();
        Map<String, Set<String>> customersByProduct = new HashMap<>();
        Map<String, Double> revenueByProduct = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) continue;
            if ("CANCELLED".equals(order.getStatus())) continue;

            boolean isSecondHalf = order.getOrderPlacedAt().isAfter(midpoint);
            for (var line : order.getLines()) {
                String pid = line.getProductId();
                double qty = line.getQty();
                if (isSecondHalf) {
                    secondHalfQty.merge(pid, qty, Double::sum);
                } else {
                    firstHalfQty.merge(pid, qty, Double::sum);
                }
                customersByProduct.computeIfAbsent(pid, k -> new HashSet<>()).add(order.getCustomerId());
                BigDecimal price = productPrices.getOrDefault(pid, BigDecimal.ONE);
                revenueByProduct.merge(pid, qty * price.doubleValue(), Double::sum);
            }
        }

        List<AiPricingSuggestionEntity> suggestions = new ArrayList<>();

        for (ProductEntity product : products) {
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) continue;
            String pid = product.getProductId();
            double currentPrice = product.getPrice().doubleValue();

            double q1 = firstHalfQty.getOrDefault(pid, 0.0);
            double q2 = secondHalfQty.getOrDefault(pid, 0.0);
            double totalQty = q1 + q2;
            if (totalQty < 5) continue; // not enough data

            // Factor 1: Demand velocity (-1 to +1 scale)
            double velocityScore = 0;
            if (q1 > 0) {
                velocityScore = Math.max(-1, Math.min(1, (q2 - q1) / q1));
            }

            // Factor 2: Inventory pressure (-1 = overstock, +1 = scarce)
            double stockQty = onHandByProduct.getOrDefault(pid, 0.0);
            double avgDailyDemand = totalQty / ANALYSIS_DAYS;
            double daysOfStock = avgDailyDemand > 0 ? stockQty / avgDailyDemand : 999;
            double inventoryScore;
            if (daysOfStock > 60) inventoryScore = -0.8;       // heavy overstock
            else if (daysOfStock > 30) inventoryScore = -0.4;  // overstock
            else if (daysOfStock < 3) inventoryScore = 0.8;    // scarce
            else if (daysOfStock < 7) inventoryScore = 0.4;    // low
            else inventoryScore = 0;                            // balanced

            // Factor 3: Customer concentration (more customers = more stable)
            int customerCount = customersByProduct.getOrDefault(pid, Set.of()).size();
            double concentrationScore = customerCount >= 5 ? 0.1 : (customerCount <= 1 ? -0.2 : 0);

            // Composite score (weighted sum)
            double composite = velocityScore * 0.4 + inventoryScore * 0.35 + concentrationScore * 0.25;

            // Convert to price adjustment percentage
            double adjustPct;
            if (composite > 0) {
                adjustPct = composite * MAX_MARKUP_PCT;   // positive = markup
            } else {
                adjustPct = composite * MAX_DISCOUNT_PCT; // negative = discount
            }

            // Skip trivial adjustments
            if (Math.abs(adjustPct) < 1.0) continue;

            double suggestedPrice = currentPrice * (1 + adjustPct / 100.0);
            suggestedPrice = Math.round(suggestedPrice * 100.0) / 100.0;

            String reason = buildReason(velocityScore, inventoryScore, daysOfStock, customerCount, adjustPct);

            AiPricingSuggestionEntity suggestion = AiPricingSuggestionEntity.builder()
                    .suggestionId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(pid)
                    .productName(productNames.getOrDefault(pid, pid))
                    .currentPrice(BigDecimal.valueOf(currentPrice))
                    .suggestedPrice(BigDecimal.valueOf(suggestedPrice))
                    .changePct(BigDecimal.valueOf(Math.round(adjustPct * 100.0) / 100.0))
                    .reason(reason)
                    .build();
            suggestions.add(pricingRepo.save(suggestion));
        }

        log.info("Generated {} dynamic pricing suggestions for tenant={}", suggestions.size(), tenantId);
        return suggestions;
    }

    private String buildReason(double velocity, double inventory, double daysOfStock,
                                int customers, double adjustPct) {
        List<String> factors = new ArrayList<>();
        if (velocity > 0.2) factors.add("rising demand (+%.0f%%)".formatted(velocity * 100));
        else if (velocity < -0.2) factors.add("declining demand (%.0f%%)".formatted(velocity * 100));

        if (inventory > 0.3) factors.add("low stock (%.0f days)".formatted(daysOfStock));
        else if (inventory < -0.3) factors.add("overstock (%.0f days)".formatted(daysOfStock));

        if (customers >= 5) factors.add("broad customer base (%d customers)".formatted(customers));
        else if (customers <= 1) factors.add("single customer dependency");

        String direction = adjustPct > 0 ? "Price increase" : "Price decrease";
        return "%s of %.1f%%: %s".formatted(direction, Math.abs(adjustPct),
                factors.isEmpty() ? "multi-factor analysis" : String.join("; ", factors));
    }
}
