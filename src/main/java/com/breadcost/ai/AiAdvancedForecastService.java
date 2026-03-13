package com.breadcost.ai;

import com.breadcost.masterdata.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * D3.1 — Advanced demand forecasting with EWMA, day-of-week seasonality, and trend analysis.
 * Replaces the simple average-based forecast in AiSuggestionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdvancedForecastService {

    private final AiDemandForecastRepository forecastRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    private static final double EWMA_ALPHA = 0.3;       // smoothing factor
    private static final int LOOKBACK_DAYS = 90;         // 3 months of history
    private static final int MIN_DATA_DAYS = 7;          // minimum days for meaningful forecast

    /**
     * Generate advanced demand forecast using exponential weighted moving average
     * with day-of-week seasonality factors and linear trend detection.
     */
    @Transactional
    public List<AiDemandForecastEntity> generateAdvancedForecast(String tenantId, int forecastDays) {
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        Map<String, String> productNames = productRepo.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        Instant cutoff = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);

        // Build daily demand matrix: productId -> [dayIndex -> qty]
        Map<String, Map<LocalDate, Double>> dailyDemand = new LinkedHashMap<>();
        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) continue;
            String status = order.getStatus();
            if (!"CONFIRMED".equals(status) && !"DELIVERED".equals(status)
                    && !"IN_PRODUCTION".equals(status) && !"READY".equals(status)) continue;

            LocalDate orderDate = order.getOrderPlacedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            for (var line : order.getLines()) {
                dailyDemand.computeIfAbsent(line.getProductId(), k -> new TreeMap<>())
                        .merge(orderDate, line.getQty(), Double::sum);
            }
        }

        LocalDate today = LocalDate.now();
        List<AiDemandForecastEntity> forecasts = new ArrayList<>();

        for (var entry : dailyDemand.entrySet()) {
            String productId = entry.getKey();
            Map<LocalDate, Double> demandByDate = entry.getValue();

            if (demandByDate.size() < MIN_DATA_DAYS) continue;

            // Fill gaps with zeros for complete daily series
            LocalDate firstDate = demandByDate.keySet().stream().min(LocalDate::compareTo).orElse(today);
            List<Double> dailySeries = new ArrayList<>();
            List<DayOfWeek> dayOfWeeks = new ArrayList<>();
            for (LocalDate d = firstDate; !d.isAfter(today); d = d.plusDays(1)) {
                dailySeries.add(demandByDate.getOrDefault(d, 0.0));
                dayOfWeeks.add(d.getDayOfWeek());
            }

            // Step 1: Compute day-of-week seasonality factors
            double[] dowTotals = new double[7];
            int[] dowCounts = new int[7];
            for (int i = 0; i < dailySeries.size(); i++) {
                int dow = dayOfWeeks.get(i).getValue() - 1; // 0=Monday
                dowTotals[dow] += dailySeries.get(i);
                dowCounts[dow]++;
            }
            double overallAvg = dailySeries.stream().mapToDouble(d -> d).average().orElse(1.0);
            double[] seasonalFactors = new double[7];
            for (int i = 0; i < 7; i++) {
                double dowAvg = dowCounts[i] > 0 ? dowTotals[i] / dowCounts[i] : overallAvg;
                seasonalFactors[i] = overallAvg > 0 ? dowAvg / overallAvg : 1.0;
            }

            // Step 2: Deseasonalize the series
            List<Double> deseasonalized = new ArrayList<>();
            for (int i = 0; i < dailySeries.size(); i++) {
                int dow = dayOfWeeks.get(i).getValue() - 1;
                double factor = seasonalFactors[dow];
                deseasonalized.add(factor > 0 ? dailySeries.get(i) / factor : dailySeries.get(i));
            }

            // Step 3: Apply EWMA to deseasonalized series
            double ewma = deseasonalized.getFirst();
            for (int i = 1; i < deseasonalized.size(); i++) {
                ewma = EWMA_ALPHA * deseasonalized.get(i) + (1 - EWMA_ALPHA) * ewma;
            }

            // Step 4: Linear trend from first half vs second half
            int half = deseasonalized.size() / 2;
            double firstHalfAvg = deseasonalized.subList(0, half).stream().mapToDouble(d -> d).average().orElse(0);
            double secondHalfAvg = deseasonalized.subList(half, deseasonalized.size()).stream()
                    .mapToDouble(d -> d).average().orElse(0);
            double dailyTrend = half > 0 ? (secondHalfAvg - firstHalfAvg) / half : 0;

            // Step 5: Project forecast with seasonality and trend
            double totalForecast = 0;
            for (int d = 1; d <= forecastDays; d++) {
                LocalDate futureDate = today.plusDays(d);
                int dow = futureDate.getDayOfWeek().getValue() - 1;
                double baseValue = ewma + dailyTrend * d;
                double seasonalValue = Math.max(0, baseValue * seasonalFactors[dow]);
                totalForecast += seasonalValue;
            }

            // Confidence based on data volume and consistency
            double cv = coefficientOfVariation(dailySeries);
            double dataBonus = Math.min(0.3, dailySeries.size() / 300.0);
            double consistencyBonus = Math.max(0, 0.2 - cv * 0.1);
            double confidence = Math.min(0.98, 0.5 + dataBonus + consistencyBonus);

            AiDemandForecastEntity fc = AiDemandForecastEntity.builder()
                    .forecastId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(productId)
                    .productName(productNames.getOrDefault(productId, productId))
                    .periodStart(today)
                    .periodEnd(today.plusDays(forecastDays))
                    .forecastQty(Math.round(totalForecast * 100.0) / 100.0)
                    .confidence(Math.round(confidence * 100.0) / 100.0)
                    .basedOnDays(LOOKBACK_DAYS)
                    .build();
            forecasts.add(forecastRepo.save(fc));
        }

        log.info("Generated {} advanced forecasts for tenant={} days={}", forecasts.size(), tenantId, forecastDays);
        return forecasts;
    }

    private double coefficientOfVariation(List<Double> values) {
        double mean = values.stream().mapToDouble(d -> d).average().orElse(0);
        if (mean == 0) return 1.0;
        double variance = values.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0);
        return Math.sqrt(variance) / mean;
    }
}
