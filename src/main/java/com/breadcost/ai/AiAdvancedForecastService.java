package com.breadcost.ai;

import com.breadcost.masterdata.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Set<String> VALID_FORECAST_STATUSES =
            Set.of("CONFIRMED", "DELIVERED", "IN_PRODUCTION", "READY");

    /**
     * Generate advanced demand forecast using exponential weighted moving average
     * with day-of-week seasonality factors and linear trend detection.
     */
    @Transactional
    public List<AiDemandForecastEntity> generateAdvancedForecast(String tenantId, int forecastDays) {
        Map<String, String> productNames = productRepo.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        Map<String, Map<LocalDate, Double>> dailyDemand = buildDailyDemandMatrix(tenantId);

        LocalDate today = LocalDate.now();
        List<AiDemandForecastEntity> forecasts = new ArrayList<>();

        for (var entry : dailyDemand.entrySet()) {
            if (entry.getValue().size() < MIN_DATA_DAYS) {
                // skip products with insufficient data
            } else {
                AiDemandForecastEntity fc = buildForecastForProduct(
                        entry.getKey(), entry.getValue(), tenantId, productNames, today, forecastDays);
                forecasts.add(forecastRepo.save(fc));
            }
        }

        log.info("Generated {} advanced forecasts for tenant={} days={}", forecasts.size(), tenantId, forecastDays);
        return forecasts;
    }

    private Map<String, Map<LocalDate, Double>> buildDailyDemandMatrix(String tenantId) {
        Instant cutoff = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<OrderEntity> orders = orderRepo.findByTenantId(tenantId);
        Map<String, Map<LocalDate, Double>> dailyDemand = new LinkedHashMap<>();

        for (OrderEntity order : orders) {
            if (order.getOrderPlacedAt() == null || order.getOrderPlacedAt().isBefore(cutoff)) {
                // skip orders outside lookback window
            } else if (!VALID_FORECAST_STATUSES.contains(order.getStatus())) {
                // skip non-qualifying statuses
            } else {
                LocalDate orderDate = order.getOrderPlacedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                for (var line : order.getLines()) {
                    dailyDemand.computeIfAbsent(line.getProductId(), k -> new TreeMap<>())
                            .merge(orderDate, line.getQty(), Double::sum);
                }
            }
        }
        return dailyDemand;
    }

    private AiDemandForecastEntity buildForecastForProduct(
            String productId, Map<LocalDate, Double> demandByDate,
            String tenantId, Map<String, String> productNames,
            LocalDate today, int forecastDays) {

        LocalDate firstDate = demandByDate.keySet().stream().min(LocalDate::compareTo).orElse(today);
        List<Double> dailySeries = new ArrayList<>();
        List<DayOfWeek> dayOfWeeks = new ArrayList<>();
        for (LocalDate d = firstDate; !d.isAfter(today); d = d.plusDays(1)) {
            dailySeries.add(demandByDate.getOrDefault(d, 0.0));
            dayOfWeeks.add(d.getDayOfWeek());
        }

        double[] seasonalFactors = computeSeasonalFactors(dailySeries, dayOfWeeks);
        List<Double> deseasonalized = deseasonalize(dailySeries, dayOfWeeks, seasonalFactors);
        double ewma = computeEwma(deseasonalized);
        double dailyTrend = computeDailyTrend(deseasonalized);
        double totalForecast = projectForecast(ewma, dailyTrend, seasonalFactors, today, forecastDays);
        double confidence = computeConfidence(dailySeries);

        return AiDemandForecastEntity.builder()
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
    }

    private double[] computeSeasonalFactors(List<Double> dailySeries, List<DayOfWeek> dayOfWeeks) {
        double[] dowTotals = new double[7];
        int[] dowCounts = new int[7];
        for (int i = 0; i < dailySeries.size(); i++) {
            int dow = dayOfWeeks.get(i).getValue() - 1;
            dowTotals[dow] += dailySeries.get(i);
            dowCounts[dow]++;
        }
        double overallAvg = dailySeries.stream().mapToDouble(d -> d).average().orElse(1.0);
        double[] seasonalFactors = new double[7];
        for (int i = 0; i < 7; i++) {
            double dowAvg = dowCounts[i] > 0 ? dowTotals[i] / dowCounts[i] : overallAvg;
            seasonalFactors[i] = overallAvg > 0 ? dowAvg / overallAvg : 1.0;
        }
        return seasonalFactors;
    }

    private List<Double> deseasonalize(List<Double> dailySeries, List<DayOfWeek> dayOfWeeks, double[] seasonalFactors) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < dailySeries.size(); i++) {
            int dow = dayOfWeeks.get(i).getValue() - 1;
            double factor = seasonalFactors[dow];
            result.add(factor > 0 ? dailySeries.get(i) / factor : dailySeries.get(i));
        }
        return result;
    }

    private double computeEwma(List<Double> deseasonalized) {
        double ewma = deseasonalized.getFirst();
        for (int i = 1; i < deseasonalized.size(); i++) {
            ewma = EWMA_ALPHA * deseasonalized.get(i) + (1 - EWMA_ALPHA) * ewma;
        }
        return ewma;
    }

    private double computeDailyTrend(List<Double> deseasonalized) {
        int half = deseasonalized.size() / 2;
        double firstHalfAvg = deseasonalized.subList(0, half).stream().mapToDouble(d -> d).average().orElse(0);
        double secondHalfAvg = deseasonalized.subList(half, deseasonalized.size()).stream()
                .mapToDouble(d -> d).average().orElse(0);
        return half > 0 ? (secondHalfAvg - firstHalfAvg) / half : 0;
    }

    private double projectForecast(double ewma, double dailyTrend, double[] seasonalFactors,
                                   LocalDate today, int forecastDays) {
        double total = 0;
        for (int d = 1; d <= forecastDays; d++) {
            int dow = today.plusDays(d).getDayOfWeek().getValue() - 1;
            double baseValue = ewma + dailyTrend * d;
            total += Math.max(0, baseValue * seasonalFactors[dow]);
        }
        return total;
    }

    private double computeConfidence(List<Double> dailySeries) {
        double cv = coefficientOfVariation(dailySeries);
        double dataBonus = Math.min(0.3, dailySeries.size() / 300.0);
        double consistencyBonus = Math.max(0, 0.2 - cv * 0.1);
        return Math.min(0.98, 0.5 + dataBonus + consistencyBonus);
    }

    private double coefficientOfVariation(List<Double> values) {
        double mean = values.stream().mapToDouble(d -> d).average().orElse(0);
        if (mean == 0) return 1.0;
        double variance = values.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0);
        return Math.sqrt(variance) / mean;
    }
}
