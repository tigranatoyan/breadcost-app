package com.breadcost.ai;

import com.breadcost.masterdata.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * D3.4 — Quality prediction service.
 * Predicts quality risk for products based on historical work order yield data,
 * recipe complexity, and production patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QualityPredictionService {

    private final AiQualityPredictionRepository predictionRepo;
    private final WorkOrderRepository workOrderRepo;
    private final ProductRepository productRepo;
    private final RecipeRepository recipeRepo;

    private static final int LOOKBACK_DAYS = 90;
    private static final double HIGH_RISK_THRESHOLD = 0.85;   // yield below 85%
    private static final double MEDIUM_RISK_THRESHOLD = 0.92; // yield below 92%

    @Transactional
    public List<AiQualityPredictionEntity> generatePredictions(String tenantId) {
        List<WorkOrderEntity> workOrders = workOrderRepo.findByTenantId(tenantId);
        List<ProductEntity> products = productRepo.findByTenantId(tenantId);

        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, ProductEntity::getName, (a, b) -> a));

        Instant cutoff = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);

        // Group completed WOs by product with yield data
        Map<String, List<WorkOrderEntity>> completedByProduct = workOrders.stream()
                .filter(wo -> "COMPLETED".equals(wo.getStatus().name()))
                .filter(wo -> wo.getCompletedAt() != null && wo.getCompletedAt().isAfter(cutoff))
                .filter(wo -> wo.getActualYield() != null && wo.getTargetQty() > 0)
                .collect(Collectors.groupingBy(WorkOrderEntity::getProductId));

        List<AiQualityPredictionEntity> predictions = new ArrayList<>();

        for (var entry : completedByProduct.entrySet()) {
            String productId = entry.getKey();
            List<WorkOrderEntity> wos = entry.getValue();

            if (wos.size() < 3) continue; // need minimum history

            // Calculate yield statistics
            List<Double> yieldPcts = wos.stream()
                    .map(wo -> wo.getActualYield() / wo.getTargetQty())
                    .toList();

            double avgYield = yieldPcts.stream().mapToDouble(d -> d).average().orElse(1.0);

            // Recent trend: last 3 WOs vs overall
            List<Double> recentYields = yieldPcts.subList(Math.max(0, yieldPcts.size() - 3), yieldPcts.size());
            double recentAvg = recentYields.stream().mapToDouble(d -> d).average().orElse(avgYield);

            // Yield variability (standard deviation)
            double variance = yieldPcts.stream()
                    .mapToDouble(y -> (y - avgYield) * (y - avgYield)).average().orElse(0);
            double stdDev = Math.sqrt(variance);

            // Waste analysis
            double avgWastePct = wos.stream()
                    .filter(wo -> wo.getWasteQty() != null)
                    .mapToDouble(wo -> wo.getWasteQty() / wo.getTargetQty())
                    .average().orElse(0);

            // Quality score analysis
            long failCount = wos.stream()
                    .filter(wo -> "FAIL".equals(wo.getQualityScore()))
                    .count();
            double failRate = (double) failCount / wos.size();

            // Recipe complexity factor
            double complexityFactor = getRecipeComplexity(tenantId, productId);

            // Composite risk score (lower = higher risk)
            double riskScore = recentAvg * 0.35 + avgYield * 0.25
                    + (1 - failRate) * 0.2 + (1 - Math.min(1, avgWastePct * 5)) * 0.1
                    + (1 - complexityFactor * 0.1) * 0.1;

            String riskLevel;
            if (riskScore < HIGH_RISK_THRESHOLD) riskLevel = "HIGH";
            else if (riskScore < MEDIUM_RISK_THRESHOLD) riskLevel = "MEDIUM";
            else riskLevel = "LOW";

            List<String> factors = buildRiskFactors(recentAvg, avgYield, stdDev, failRate, avgWastePct, complexityFactor);
            String recommendation = buildRecommendation(riskLevel, recentAvg, avgYield);

            double confidence = Math.min(0.95, 0.4 + wos.size() / 50.0 + (1 - stdDev) * 0.2);

            AiQualityPredictionEntity prediction = AiQualityPredictionEntity.builder()
                    .predictionId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .productId(productId)
                    .productName(productNames.getOrDefault(productId, productId))
                    .riskLevel(riskLevel)
                    .predictedYieldPct(Math.round(recentAvg * 10000.0) / 100.0)
                    .historicalAvgYieldPct(Math.round(avgYield * 10000.0) / 100.0)
                    .riskFactors(String.join("; ", factors))
                    .recommendation(recommendation)
                    .confidence(Math.round(confidence * 100.0) / 100.0)
                    .build();
            predictions.add(predictionRepo.save(prediction));
        }

        log.info("Generated {} quality predictions for tenant={}", predictions.size(), tenantId);
        return predictions;
    }

    public List<AiQualityPredictionEntity> getPredictions(String tenantId) {
        return predictionRepo.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public List<AiQualityPredictionEntity> getHighRiskPredictions(String tenantId) {
        return predictionRepo.findByTenantIdAndRiskLevel(tenantId, "HIGH");
    }

    @Transactional
    public AiQualityPredictionEntity dismissPrediction(String predictionId) {
        AiQualityPredictionEntity p = predictionRepo.findById(predictionId)
                .orElseThrow(() -> new NoSuchElementException("Prediction not found: " + predictionId));
        p.setStatus("DISMISSED");
        return predictionRepo.save(p);
    }

    private double getRecipeComplexity(String tenantId, String productId) {
        List<RecipeEntity> recipes = recipeRepo.findByTenantIdAndProductId(tenantId, productId);
        if (recipes.isEmpty()) return 0.5;
        // Complexity based on ingredient count of the active recipe
        return recipes.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .findFirst()
                .map(r -> Math.min(1.0, r.getIngredients().size() / 10.0))
                .orElse(0.5);
    }

    private List<String> buildRiskFactors(double recentAvg, double avgYield, double stdDev,
                                           double failRate, double wastePct, double complexity) {
        List<String> factors = new ArrayList<>();
        if (recentAvg < avgYield - 0.05) factors.add("declining yield trend (%.1f%% vs %.1f%% avg)"
                .formatted(recentAvg * 100, avgYield * 100));
        if (stdDev > 0.1) factors.add("high yield variability (σ=%.2f)".formatted(stdDev));
        if (failRate > 0.1) factors.add("%.0f%% QA failure rate".formatted(failRate * 100));
        if (wastePct > 0.05) factors.add("high waste (%.1f%%)".formatted(wastePct * 100));
        if (complexity > 0.7) factors.add("complex recipe (%d ingredients)".formatted(Math.round(complexity * 10)));
        if (factors.isEmpty()) factors.add("within normal parameters");
        return factors;
    }

    private String buildRecommendation(String riskLevel,
                                        double recentAvg, double histAvg) {
        return switch (riskLevel) {
            case "HIGH" -> "Immediate attention required. Consider recipe review, ingredient quality checks, " +
                    "or additional operator training. Recent yield at %.1f%%.".formatted(recentAvg * 100);
            case "MEDIUM" -> ("Monitor closely. Recent yield (%.1f%%) is below target (%.1f%%). " +
                    "Review recent production logs for patterns.").formatted(recentAvg * 100, histAvg * 100);
            default -> "Quality within acceptable range. Continue standard monitoring.";
        };
    }
}
