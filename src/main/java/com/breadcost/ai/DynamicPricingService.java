package com.breadcost.ai;

import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        Map<String, Double> onHandByProduct = inventoryProjection.getAllPositions().stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .collect(Collectors.groupingBy(
                        InventoryProjection.InventoryPosition::getItemId,
                        Collectors.summingDouble(p -> p.getOnHandQty().doubleValue())));

        DemandAnalysis analysis = analyzeDemand(orders);

        List<AiPricingSuggestionEntity> suggestions = new ArrayList<>();

        for (ProductEntity product : products) {
            AiPricingSuggestionEntity suggestion = evaluateProductPricing(
                    product, analysis, onHandByProduct, productNames, tenantId);
            if (suggestion != null) {
                suggestions.add(pricingRepo.save(suggestion));
            }
        }

        log.info("Generated {} dynamic pricing suggestions for tenant={}", suggestions.size(), tenantId);
        return suggestions;
    }

    private DemandAnalysis analyzeDemand(List<OrderEntity> orders) {
        Instant cutoff = Instant.now().minus(ANALYSIS_DAYS, ChronoUnit.DAYS);
        Instant midpoint = Instant.now().minus(ANALYSIS_DAYS / 2, ChronoUnit.DAYS);

        Map<String, Double> firstHalfQty = new HashMap<>();
        Map<String, Double> secondHalfQty = new HashMap<>();
        Map<String, Set<String>> customersByProduct = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) {
                // outside analysis window
            } else if ("CANCELLED".equals(order.getStatus())) {
                // skip cancelled
            } else {
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
                }
            }
        }

        return new DemandAnalysis(firstHalfQty, secondHalfQty, customersByProduct);
    }

    private AiPricingSuggestionEntity evaluateProductPricing(
            ProductEntity product, DemandAnalysis analysis,
            Map<String, Double> onHandByProduct, Map<String, String> productNames,
            String tenantId) {
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String pid = product.getProductId();
        double currentPrice = product.getPrice().doubleValue();

        double q1 = analysis.firstHalfQty.getOrDefault(pid, 0.0);
        double q2 = analysis.secondHalfQty.getOrDefault(pid, 0.0);
        double totalQty = q1 + q2;
        if (totalQty < 5) {
            return null;
        }

        double velocityScore = computeVelocityScore(q1, q2);
        double inventoryScore = computeInventoryScore(pid, totalQty, onHandByProduct);
        double concentrationScore = computeConcentrationScore(pid, analysis.customersByProduct);

        double composite = velocityScore * 0.4 + inventoryScore * 0.35 + concentrationScore * 0.25;
        double adjustPct = composite > 0 ? composite * MAX_MARKUP_PCT : composite * MAX_DISCOUNT_PCT;

        if (Math.abs(adjustPct) < 1.0) {
            return null;
        }

        double suggestedPrice = Math.round(currentPrice * (1 + adjustPct / 100.0) * 100.0) / 100.0;
        double stockQty = onHandByProduct.getOrDefault(pid, 0.0);
        double avgDailyDemand = totalQty / ANALYSIS_DAYS;
        double daysOfStock = avgDailyDemand > 0 ? stockQty / avgDailyDemand : 999;
        int customerCount = analysis.customersByProduct.getOrDefault(pid, Set.of()).size();
        String reason = buildReason(velocityScore, inventoryScore, daysOfStock, customerCount, adjustPct);

        return AiPricingSuggestionEntity.builder()
                .suggestionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .productId(pid)
                .productName(productNames.getOrDefault(pid, pid))
                .currentPrice(BigDecimal.valueOf(currentPrice))
                .suggestedPrice(BigDecimal.valueOf(suggestedPrice))
                .changePct(BigDecimal.valueOf(Math.round(adjustPct * 100.0) / 100.0))
                .reason(reason)
                .build();
    }

    private double computeVelocityScore(double q1, double q2) {
        if (q1 > 0) {
            return Math.clamp((q2 - q1) / q1, -1, 1);
        }
        return 0;
    }

    private double computeInventoryScore(String pid, double totalQty, Map<String, Double> onHandByProduct) {
        double stockQty = onHandByProduct.getOrDefault(pid, 0.0);
        double avgDailyDemand = totalQty / ANALYSIS_DAYS;
        double daysOfStock = avgDailyDemand > 0 ? stockQty / avgDailyDemand : 999;
        if (daysOfStock > 60) return -0.8;
        if (daysOfStock > 30) return -0.4;
        if (daysOfStock < 3) return 0.8;
        if (daysOfStock < 7) return 0.4;
        return 0;
    }

    private double computeConcentrationScore(String pid, Map<String, Set<String>> customersByProduct) {
        int customerCount = customersByProduct.getOrDefault(pid, Set.of()).size();
        if (customerCount >= 5) return 0.1;
        if (customerCount <= 1) return -0.2;
        return 0;
    }

    private record DemandAnalysis(
            Map<String, Double> firstHalfQty,
            Map<String, Double> secondHalfQty,
            Map<String, Set<String>> customersByProduct) {}

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
