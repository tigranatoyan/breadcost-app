package com.breadcost.ai;

import com.breadcost.eventstore.EventStore;
import com.breadcost.eventstore.StoredEvent;
import com.breadcost.masterdata.*;
import com.breadcost.projections.InventoryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Suggestion service — BC-1901 (replenishment), BC-1902 (production), BC-1903 (demand forecast).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiSuggestionService {

    private final AiReplenishmentHintRepository hintRepo;
    private final AiDemandForecastRepository forecastRepo;
    private final AiProductionSuggestionRepository prodSugRepo;
    private final InventoryProjection inventoryProjection;
    private final EventStore eventStore;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final RecipeRepository recipeRepo;

    // ── BC-1901: Replenishment Hints (FR-12.3) ───────────────────────────────

    /**
     * Generate replenishment hints based on current stock and historical consumption.
     */
    @Transactional
    public List<AiReplenishmentHintEntity> generateReplenishmentHints(String tenantId, String period) {
        int periodDays = "MONTHLY".equalsIgnoreCase(period) ? 30 : 7;

        // Get current inventory positions
        var positions = inventoryProjection.getAllPositions().stream()
                .filter(p -> tenantId.equals(p.getTenantId()))
                .collect(Collectors.toList());

        // Compute consumption rates from IssueToBatch events
        Map<String, Double> dailyConsumption = computeDailyConsumption(tenantId, 30);

        List<AiReplenishmentHintEntity> hints = new ArrayList<>();

        // Aggregate on-hand qty per itemId
        Map<String, Double> onHandByItem = positions.stream()
                .collect(Collectors.groupingBy(
                        InventoryProjection.InventoryPosition::getItemId,
                        Collectors.summingDouble(p -> p.getOnHandQty().doubleValue())));

        // Also collect unit info
        Map<String, String> unitByItem = positions.stream()
                .collect(Collectors.toMap(
                        InventoryProjection.InventoryPosition::getItemId,
                        InventoryProjection.InventoryPosition::getUom,
                        (a, b) -> a));

        for (var entry : onHandByItem.entrySet()) {
            String itemId = entry.getKey();
            double onHand = entry.getValue();
            double dailyUse = dailyConsumption.getOrDefault(itemId, 0.0);

            double daysLeft = dailyUse > 0 ? onHand / dailyUse : 999;
            double neededForPeriod = dailyUse * periodDays;
            double suggestedQty = Math.max(0, neededForPeriod - onHand);

            if (suggestedQty <= 0) continue; // sufficient stock

            AiReplenishmentHintEntity hint = AiReplenishmentHintEntity.builder()
                    .hintId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .itemId(itemId)
                    .currentQty(onHand)
                    .avgDailyUse(Math.round(dailyUse * 100.0) / 100.0)
                    .daysLeft(Math.round(daysLeft * 10.0) / 10.0)
                    .suggestedQty(Math.ceil(suggestedQty))
                    .unit(unitByItem.getOrDefault(itemId, "units"))
                    .period(period.toUpperCase())
                    .build();
            hints.add(hintRepo.save(hint));
        }

        log.info("Generated {} replenishment hints for tenant={} period={}", hints.size(), tenantId, period);
        return hints;
    }

    public List<AiReplenishmentHintEntity> getHints(String tenantId) {
        return hintRepo.findByTenantId(tenantId);
    }

    public List<AiReplenishmentHintEntity> getPendingHints(String tenantId) {
        return hintRepo.findByTenantIdAndStatus(tenantId, "PENDING");
    }

    @Transactional
    public AiReplenishmentHintEntity dismissHint(String hintId) {
        AiReplenishmentHintEntity hint = hintRepo.findById(hintId)
                .orElseThrow(() -> new NoSuchElementException("Hint not found: " + hintId));
        hint.setStatus("DISMISSED");
        return hintRepo.save(hint);
    }

    // ── BC-1903: Demand Forecasting (FR-12.7) ────────────────────────────────

    /**
     * Generate demand forecast per product for the next period.
     */
    @Transactional
    public List<AiDemandForecastEntity> generateDemandForecast(String tenantId, int forecastDays) {
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);

        // Build per-product demand from CONFIRMED/DELIVERED orders
        Map<String, Double> totalQtyByProduct = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) continue;
            String status = order.getStatus();
            if (!"CONFIRMED".equals(status) && !"DELIVERED".equals(status)
                    && !"IN_PRODUCTION".equals(status) && !"READY".equals(status)) continue;

            for (var line : order.getLines()) {
                totalQtyByProduct.merge(line.getProductId(), line.getQty(), Double::sum);
                productNames.putIfAbsent(line.getProductId(), line.getProductName());
            }
        }

        LocalDate now = LocalDate.now();
        List<AiDemandForecastEntity> forecasts = new ArrayList<>();

        for (var entry : totalQtyByProduct.entrySet()) {
            double dailyAvg = entry.getValue() / 30.0;
            double forecastQty = dailyAvg * forecastDays;
            double confidence = Math.min(0.95, 0.5 + (entry.getValue() / 500.0));

            AiDemandForecastEntity fc = AiDemandForecastEntity.builder()
                    .forecastId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(entry.getKey())
                    .productName(productNames.get(entry.getKey()))
                    .periodStart(now)
                    .periodEnd(now.plusDays(forecastDays))
                    .forecastQty(Math.round(forecastQty * 100.0) / 100.0)
                    .confidence(Math.round(confidence * 100.0) / 100.0)
                    .basedOnDays(30)
                    .build();
            forecasts.add(forecastRepo.save(fc));
        }

        log.info("Generated {} demand forecasts for tenant={} days={}", forecasts.size(), tenantId, forecastDays);
        return forecasts;
    }

    public List<AiDemandForecastEntity> getForecasts(String tenantId) {
        return forecastRepo.findByTenantId(tenantId);
    }

    // ── BC-1902: Production Suggestions (FR-12.4) ────────────────────────────

    /**
     * Generate production suggestions based on demand forecast and recipe batch sizes.
     */
    @Transactional
    public List<AiProductionSuggestionEntity> generateProductionSuggestions(String tenantId, LocalDate planDate) {
        // Get recent forecasts or compute inline
        List<AiDemandForecastEntity> forecasts = forecastRepo.findByTenantId(tenantId);
        if (forecasts.isEmpty()) {
            forecasts = generateDemandForecast(tenantId, 7);
        }

        List<AiProductionSuggestionEntity> suggestions = new ArrayList<>();

        for (AiDemandForecastEntity fc : forecasts) {
            // Look up recipe for batch size
            var recipes = recipeRepo.findByTenantIdAndProductId(tenantId, fc.getProductId());
            BigDecimal batchSize = BigDecimal.ONE;
            if (!recipes.isEmpty()) {
                batchSize = recipes.getFirst().getBatchSize();
                if (batchSize.compareTo(BigDecimal.ZERO) <= 0) batchSize = BigDecimal.ONE;
            }

            // Scale forecast to daily need for planDate
            long daysInForecast = ChronoUnit.DAYS.between(fc.getPeriodStart(), fc.getPeriodEnd());
            double dailyNeed = daysInForecast > 0 ? fc.getForecastQty() / daysInForecast : fc.getForecastQty();
            int batches = BigDecimal.valueOf(dailyNeed)
                    .divide(batchSize, 0, RoundingMode.CEILING).intValue();
            if (batches <= 0) continue;

            String reason = String.format("Forecast %.1f/day over %dd, batch size %.1f → %d batches",
                    dailyNeed, daysInForecast, batchSize.doubleValue(), batches);

            AiProductionSuggestionEntity sug = AiProductionSuggestionEntity.builder()
                    .suggestionId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(fc.getProductId())
                    .productName(fc.getProductName())
                    .suggestedQty(dailyNeed)
                    .suggestedBatches(batches)
                    .reason(reason)
                    .planDate(planDate)
                    .build();
            suggestions.add(prodSugRepo.save(sug));
        }

        log.info("Generated {} production suggestions for tenant={} date={}", suggestions.size(), tenantId, planDate);
        return suggestions;
    }

    public List<AiProductionSuggestionEntity> getSuggestions(String tenantId, LocalDate planDate) {
        return prodSugRepo.findByTenantIdAndPlanDate(tenantId, planDate);
    }

    public List<AiProductionSuggestionEntity> getAllSuggestions(String tenantId) {
        return prodSugRepo.findByTenantId(tenantId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Double> computeDailyConsumption(String tenantId, int lookbackDays) {
        Instant cutoff = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        Map<String, Double> totalByItem = new HashMap<>();

        for (StoredEvent se : eventStore.getAllEvents()) {
            if (!"IssueToBatch".equals(se.getEventType())) continue;
            if (se.getRecordedAtUtc().isBefore(cutoff)) continue;

            var event = se.getEvent();
            // IssueToBatchEvent has tenantId, itemId, qty fields via DomainEvent
            // Use reflection-free approach: check event properties
            if (event instanceof com.breadcost.events.IssueToBatchEvent ibe) {
                if (!tenantId.equals(ibe.getTenantId())) continue;
                totalByItem.merge(ibe.getItemId(), ibe.getQty().doubleValue(), Double::sum);
            }
        }

        // Convert totals to daily averages
        Map<String, Double> daily = new HashMap<>();
        for (var entry : totalByItem.entrySet()) {
            daily.put(entry.getKey(), entry.getValue() / lookbackDays);
        }
        return daily;
    }
}
