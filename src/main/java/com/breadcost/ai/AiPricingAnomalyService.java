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
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);

        // Build product price map
        Map<String, BigDecimal> productPrices = products.stream()
                .filter(p -> p.getPrice() != null)
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getPrice, (a, b) -> a));

        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        // Analyze orders from last 90 days for volume-based pricing
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        Map<String, Double> totalQtyByProduct = new HashMap<>();
        Map<String, Set<String>> customersByProduct = new HashMap<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) continue;
            if ("CANCELLED".equals(order.getStatus())) continue;

            for (var line : order.getLines()) {
                totalQtyByProduct.merge(line.getProductId(), line.getQty(), Double::sum);
                customersByProduct.computeIfAbsent(line.getProductId(), k -> new HashSet<>())
                        .add(order.getCustomerId());
            }
        }

        List<AiPricingSuggestionEntity> suggestions = new ArrayList<>();

        for (var entry : totalQtyByProduct.entrySet()) {
            String productId = entry.getKey();
            double totalQty = entry.getValue();
            BigDecimal currentPrice = productPrices.get(productId);
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            int customerCount = customersByProduct.getOrDefault(productId, Set.of()).size();

            // High-volume products: suggest slight discount to incentivize bulk
            // Low-demand products: suggest markup or promotion
            BigDecimal suggestedPrice;
            String reason;

            if (totalQty > 500 && customerCount > 5) {
                // Popular product — suggest small volume discount
                BigDecimal discount = BigDecimal.valueOf(0.03); // 3%
                suggestedPrice = currentPrice.multiply(BigDecimal.ONE.subtract(discount))
                        .setScale(4, RoundingMode.HALF_UP);
                reason = String.format("High demand (%.0f units, %d customers in 90d) — " +
                        "3%% volume discount suggested to boost retention", totalQty, customerCount);
            } else if (totalQty < 50 && customerCount <= 2) {
                // Low-demand product — suggest price review
                BigDecimal markup = BigDecimal.valueOf(0.05); // 5%
                suggestedPrice = currentPrice.multiply(BigDecimal.ONE.add(markup))
                        .setScale(4, RoundingMode.HALF_UP);
                reason = String.format("Low demand (%.0f units, %d customers in 90d) — " +
                        "5%% markup suggested or consider promotional campaign", totalQty, customerCount);
            } else {
                continue; // No adjustment needed
            }

            BigDecimal changePct = suggestedPrice.subtract(currentPrice)
                    .divide(currentPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            AiPricingSuggestionEntity sug = AiPricingSuggestionEntity.builder()
                    .suggestionId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(productId)
                    .productName(productNames.get(productId))
                    .currentPrice(currentPrice)
                    .suggestedPrice(suggestedPrice)
                    .changePct(changePct)
                    .reason(reason)
                    .build();
            suggestions.add(pricingRepo.save(sug));
        }

        log.info("Generated {} pricing suggestions for tenant={}", suggestions.size(), tenantId);
        return suggestions;
    }

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

        // Split orders into baseline period (8-37 days ago) and recent (0-7 days)
        List<OrderEntity> recentOrders = new ArrayList<>();
        List<OrderEntity> baselineOrders = new ArrayList<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || "CANCELLED".equals(order.getStatus())) continue;
            if (order.getOrderPlacedAt().isAfter(recentCutoff)) {
                recentOrders.add(order);
            } else if (order.getOrderPlacedAt().isAfter(baselineCutoff)) {
                baselineOrders.add(order);
            }
        }

        // Revenue anomaly
        double recentRevenue = recentOrders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();
        double baselineRevenue = baselineOrders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();
        double baselineWeeklyAvg = baselineRevenue / 4.0; // 30 days ≈ 4 weeks

        if (baselineWeeklyAvg > 0) {
            double revenueDeviation = (recentRevenue - baselineWeeklyAvg) / baselineWeeklyAvg * 100;
            if (Math.abs(revenueDeviation) > 20) {
                String direction = revenueDeviation < 0 ? "drop" : "spike";
                String severity = Math.abs(revenueDeviation) > 40 ? "CRITICAL" : "WARNING";

                alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                        .alertId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .alertType("REVENUE_" + direction.toUpperCase())
                        .severity(severity)
                        .metricName("Weekly Revenue")
                        .expectedValue(BigDecimal.valueOf(baselineWeeklyAvg).setScale(4, RoundingMode.HALF_UP))
                        .actualValue(BigDecimal.valueOf(recentRevenue).setScale(4, RoundingMode.HALF_UP))
                        .deviationPct(BigDecimal.valueOf(revenueDeviation).setScale(4, RoundingMode.HALF_UP))
                        .explanation(String.format("Revenue %s of %.1f%% detected vs 4-week average", direction, Math.abs(revenueDeviation)))
                        .suggestedAction(revenueDeviation < 0
                                ? "Review recent order trends, check for supply issues or lost customers"
                                : "Investigate cause of revenue increase — seasonal, promotion, or new customers")
                        .reportContext("dashboard")
                        .build()));
            }
        }

        // Order volume anomaly
        double recentVolume = recentOrders.size();
        double baselineWeeklyVolume = baselineOrders.size() / 4.0;

        if (baselineWeeklyVolume > 0) {
            double volumeDeviation = (recentVolume - baselineWeeklyVolume) / baselineWeeklyVolume * 100;
            if (Math.abs(volumeDeviation) > 25) {
                String direction = volumeDeviation < 0 ? "drop" : "spike";
                alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                        .alertId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .alertType("ORDER_VOLUME_" + direction.toUpperCase())
                        .severity("WARNING")
                        .metricName("Weekly Order Count")
                        .expectedValue(BigDecimal.valueOf(baselineWeeklyVolume).setScale(4, RoundingMode.HALF_UP))
                        .actualValue(BigDecimal.valueOf(recentVolume).setScale(4, RoundingMode.HALF_UP))
                        .deviationPct(BigDecimal.valueOf(volumeDeviation).setScale(4, RoundingMode.HALF_UP))
                        .explanation(String.format("Order volume %s of %.1f%% vs 4-week average", direction, Math.abs(volumeDeviation)))
                        .suggestedAction("Review customer activity and marketing efforts")
                        .reportContext("dashboard")
                        .build()));
            }
        }

        // Average order value anomaly
        double recentAov = recentOrders.isEmpty() ? 0 :
                recentOrders.stream()
                        .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                        .average().orElse(0);
        double baselineAov = baselineOrders.isEmpty() ? 0 :
                baselineOrders.stream()
                        .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                        .average().orElse(0);

        if (baselineAov > 0) {
            double aovDeviation = (recentAov - baselineAov) / baselineAov * 100;
            if (Math.abs(aovDeviation) > 15) {
                String direction = aovDeviation < 0 ? "decrease" : "increase";
                alerts.add(anomalyRepo.save(AiAnomalyAlertEntity.builder()
                        .alertId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .alertType("AOV_" + direction.toUpperCase())
                        .severity("INFO")
                        .metricName("Average Order Value")
                        .expectedValue(BigDecimal.valueOf(baselineAov).setScale(4, RoundingMode.HALF_UP))
                        .actualValue(BigDecimal.valueOf(recentAov).setScale(4, RoundingMode.HALF_UP))
                        .deviationPct(BigDecimal.valueOf(aovDeviation).setScale(4, RoundingMode.HALF_UP))
                        .explanation(String.format("Average order value %s of %.1f%%", direction, Math.abs(aovDeviation)))
                        .suggestedAction("Check product mix changes, pricing impact, or customer segment shifts")
                        .reportContext("reports")
                        .build()));
            }
        }

        log.info("Generated {} anomaly alerts for tenant={}", alerts.size(), tenantId);
        return alerts;
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
