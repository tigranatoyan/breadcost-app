package com.breadcost.ai;

import com.breadcost.masterdata.*;
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
 * AI pricing + anomaly service — BC-2001 (FR-12.5), BC-2002 (FR-12.6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPricingAnomalyService {

    private final AiPricingSuggestionRepository pricingRepo;
    private final AiAnomalyAlertRepository anomalyRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    // ── BC-2001: Pricing Suggestions (FR-12.5) ──────────────────────────────

    @Transactional
    public List<AiPricingSuggestionEntity> generatePricingSuggestions(String tenantId) {
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);

        Map<String, BigDecimal> productPrices = products.stream()
                .filter(p -> p.getPrice() != null)
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getPrice, (a, b) -> a));

        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        OrderDemandSummary demandSummary = summarizeOrderDemand(tenantId);

        List<AiPricingSuggestionEntity> suggestions = new ArrayList<>();

        for (var entry : demandSummary.totalQtyByProduct.entrySet()) {
            AiPricingSuggestionEntity sug = evaluatePricing(
                    entry.getKey(), entry.getValue(),
                    demandSummary.customersByProduct, productPrices, productNames, tenantId);
            if (sug != null) {
                suggestions.add(pricingRepo.save(sug));
            }
        }

        log.info("Generated {} pricing suggestions for tenant={}", suggestions.size(), tenantId);
        return suggestions;
    }

    private OrderDemandSummary summarizeOrderDemand(String tenantId) {
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        Map<String, Double> totalQtyByProduct = new HashMap<>();
        Map<String, Set<String>> customersByProduct = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) {
                // outside analysis window
            } else if ("CANCELLED".equals(order.getStatus())) {
                // skip cancelled
            } else {
                for (var line : order.getLines()) {
                    totalQtyByProduct.merge(line.getProductId(), line.getQty(), Double::sum);
                    customersByProduct.computeIfAbsent(line.getProductId(), k -> new HashSet<>())
                            .add(order.getCustomerId());
                }
            }
        }

        return new OrderDemandSummary(totalQtyByProduct, customersByProduct);
    }

    private AiPricingSuggestionEntity evaluatePricing(
            String productId, double totalQty,
            Map<String, Set<String>> customersByProduct,
            Map<String, BigDecimal> productPrices,
            Map<String, String> productNames,
            String tenantId) {

        BigDecimal currentPrice = productPrices.get(productId);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        int customerCount = customersByProduct.getOrDefault(productId, Set.of()).size();

        BigDecimal suggestedPrice;
        String reason;

        if (totalQty > 500 && customerCount > 5) {
            BigDecimal discount = BigDecimal.valueOf(0.03);
            suggestedPrice = currentPrice.multiply(BigDecimal.ONE.subtract(discount))
                    .setScale(4, RoundingMode.HALF_UP);
            reason = String.format("High demand (%.0f units, %d customers in 90d) — " +
                    "3%% volume discount suggested to boost retention", totalQty, customerCount);
        } else if (totalQty < 50 && customerCount <= 2) {
            BigDecimal markup = BigDecimal.valueOf(0.05);
            suggestedPrice = currentPrice.multiply(BigDecimal.ONE.add(markup))
                    .setScale(4, RoundingMode.HALF_UP);
            reason = String.format("Low demand (%.0f units, %d customers in 90d) — " +
                    "5%% markup suggested or consider promotional campaign", totalQty, customerCount);
        } else {
            return null;
        }

        BigDecimal changePct = suggestedPrice.subtract(currentPrice)
                .divide(currentPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return AiPricingSuggestionEntity.builder()
                .suggestionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .productId(productId)
                .productName(productNames.get(productId))
                .currentPrice(currentPrice)
                .suggestedPrice(suggestedPrice)
                .changePct(changePct)
                .reason(reason)
                .build();
    }

    private record OrderDemandSummary(
            Map<String, Double> totalQtyByProduct,
            Map<String, Set<String>> customersByProduct) {}

    public List<AiPricingSuggestionEntity> getPricingSuggestions(String tenantId) {
        return pricingRepo.findByTenantId(tenantId);
    }

    public List<AiPricingSuggestionEntity> getPendingPricingSuggestions(String tenantId) {
        return pricingRepo.findByTenantIdAndStatus(tenantId, "PENDING");
    }

    @Transactional
    public AiPricingSuggestionEntity dismissPricingSuggestion(String suggestionId) {
        AiPricingSuggestionEntity sug = pricingRepo.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("Pricing suggestion not found: " + suggestionId));
        sug.setStatus("DISMISSED");
        return pricingRepo.save(sug);
    }

    @Transactional
    public AiPricingSuggestionEntity acceptPricingSuggestion(String suggestionId) {
        AiPricingSuggestionEntity sug = pricingRepo.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("Pricing suggestion not found: " + suggestionId));
        sug.setStatus("ACCEPTED");
        return pricingRepo.save(sug);
    }

    // ── BC-2002: Anomaly Alerts (FR-12.6) ────────────────────────────────────

    @Transactional
    public List<AiAnomalyAlertEntity> generateAnomalyAlerts(String tenantId) {
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        List<AiAnomalyAlertEntity> alerts = new ArrayList<>();

        Instant now = Instant.now();
        Instant recentCutoff = now.minus(7, ChronoUnit.DAYS);
        Instant baselineCutoff = now.minus(37, ChronoUnit.DAYS);

        List<OrderEntity> recentOrders = new ArrayList<>();
        List<OrderEntity> baselineOrders = new ArrayList<>();
        partitionOrders(orders, recentCutoff, baselineCutoff, recentOrders, baselineOrders);

        checkRevenueAnomaly(tenantId, recentOrders, baselineOrders, alerts);
        checkOrderVolumeAnomaly(tenantId, recentOrders, baselineOrders, alerts);
        checkAovAnomaly(tenantId, recentOrders, baselineOrders, alerts);

        log.info("Generated {} anomaly alerts for tenant={}", alerts.size(), tenantId);
        return alerts;
    }

    private void partitionOrders(List<OrderEntity> orders,
                                 Instant recentCutoff, Instant baselineCutoff,
                                 List<OrderEntity> recentOrders, List<OrderEntity> baselineOrders) {
        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || "CANCELLED".equals(order.getStatus())) {
                // skip
            } else if (order.getOrderPlacedAt().isAfter(recentCutoff)) {
                recentOrders.add(order);
            } else if (order.getOrderPlacedAt().isAfter(baselineCutoff)) {
                baselineOrders.add(order);
            }
        }
    }

    private double sumRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();
    }

    private double avgOrderValue(List<OrderEntity> orders) {
        if (orders.isEmpty()) return 0;
        return orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .average().orElse(0);
    }

    private void checkRevenueAnomaly(String tenantId,
                                     List<OrderEntity> recentOrders, List<OrderEntity> baselineOrders,
                                     List<AiAnomalyAlertEntity> alerts) {
        double recentRevenue = sumRevenue(recentOrders);
        double baselineWeeklyAvg = sumRevenue(baselineOrders) / 4.0;
        if (baselineWeeklyAvg <= 0) return;

        double deviation = (recentRevenue - baselineWeeklyAvg) / baselineWeeklyAvg * 100;
        if (Math.abs(deviation) <= 20) return;

        String direction = deviation < 0 ? "drop" : "spike";
        String severity = Math.abs(deviation) > 40 ? "CRITICAL" : "WARNING";

        alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                .alertId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .alertType("REVENUE_" + direction.toUpperCase())
                .severity(severity)
                .metricName("Weekly Revenue")
                .expectedValue(BigDecimal.valueOf(baselineWeeklyAvg).setScale(4, RoundingMode.HALF_UP))
                .actualValue(BigDecimal.valueOf(recentRevenue).setScale(4, RoundingMode.HALF_UP))
                .deviationPct(BigDecimal.valueOf(deviation).setScale(4, RoundingMode.HALF_UP))
                .explanation(String.format("Revenue %s of %.1f%% detected vs 4-week average", direction, Math.abs(deviation)))
                .suggestedAction(deviation < 0
                        ? "Review recent order trends, check for supply issues or lost customers"
                        : "Investigate cause of revenue increase — seasonal, promotion, or new customers")
                .reportContext("dashboard")
                .build()));
    }

    private void checkOrderVolumeAnomaly(String tenantId,
                                         List<OrderEntity> recentOrders, List<OrderEntity> baselineOrders,
                                         List<AiAnomalyAlertEntity> alerts) {
        double recentVolume = recentOrders.size();
        double baselineWeeklyVolume = baselineOrders.size() / 4.0;
        if (baselineWeeklyVolume <= 0) return;

        double deviation = (recentVolume - baselineWeeklyVolume) / baselineWeeklyVolume * 100;
        if (Math.abs(deviation) <= 25) return;

        String direction = deviation < 0 ? "drop" : "spike";
        alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                .alertId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .alertType("ORDER_VOLUME_" + direction.toUpperCase())
                .severity("WARNING")
                .metricName("Weekly Order Count")
                .expectedValue(BigDecimal.valueOf(baselineWeeklyVolume).setScale(4, RoundingMode.HALF_UP))
                .actualValue(BigDecimal.valueOf(recentVolume).setScale(4, RoundingMode.HALF_UP))
                .deviationPct(BigDecimal.valueOf(deviation).setScale(4, RoundingMode.HALF_UP))
                .explanation(String.format("Order volume %s of %.1f%% vs 4-week average", direction, Math.abs(deviation)))
                .suggestedAction("Review customer activity and marketing efforts")
                .reportContext("dashboard")
                .build()));
    }

    private void checkAovAnomaly(String tenantId,
                                 List<OrderEntity> recentOrders, List<OrderEntity> baselineOrders,
                                 List<AiAnomalyAlertEntity> alerts) {
        double recentAov = avgOrderValue(recentOrders);
        double baselineAov = avgOrderValue(baselineOrders);
        if (baselineAov <= 0) return;

        double deviation = (recentAov - baselineAov) / baselineAov * 100;
        if (Math.abs(deviation) <= 15) return;

        String direction = deviation < 0 ? "decrease" : "increase";
        alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                .alertId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .alertType("AOV_" + direction.toUpperCase())
                .severity("INFO")
                .metricName("Average Order Value")
                .expectedValue(BigDecimal.valueOf(baselineAov).setScale(4, RoundingMode.HALF_UP))
                .actualValue(BigDecimal.valueOf(recentAov).setScale(4, RoundingMode.HALF_UP))
                .deviationPct(BigDecimal.valueOf(deviation).setScale(4, RoundingMode.HALF_UP))
                .explanation(String.format("Average order value %s of %.1f%%", direction, Math.abs(deviation)))
                .suggestedAction("Check product mix changes, pricing impact, or customer segment shifts")
                .reportContext("reports")
                .build()));
    }

    public List<AiAnomalyAlertEntity> getAlerts(String tenantId) {
        return anomalyRepo.findByTenantId(tenantId);
    }

    public List<AiAnomalyAlertEntity> getActiveAlerts(String tenantId) {
        return anomalyRepo.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    @Transactional
    public AiAnomalyAlertEntity acknowledgeAlert(String alertId) {
        AiAnomalyAlertEntity alert = anomalyRepo.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
        alert.setStatus("ACKNOWLEDGED");
        return anomalyRepo.save(alert);
    }

    @Transactional
    public AiAnomalyAlertEntity dismissAlert(String alertId) {
        AiAnomalyAlertEntity alert = anomalyRepo.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
        alert.setStatus("DISMISSED");
        return anomalyRepo.save(alert);
    }
}
