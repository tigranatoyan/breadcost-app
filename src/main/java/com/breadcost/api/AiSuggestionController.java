package com.breadcost.api;

import com.breadcost.ai.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI suggestion endpoints — BC-1901..1903 (FR-12.3, FR-12.4, FR-12.7)
 */
@Tag(name = "AI Suggestions", description = "AI-driven replenishment and forecast suggestions")
@RestController
@RequestMapping("/v3/ai/suggestions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager')")
public class AiSuggestionController {

    private final AiSuggestionService service;

    // ── BC-1901: Replenishment Hints ─────────────────────────────────────────

    @PostMapping("/replenishment/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiReplenishmentHintEntity> generateHints(@RequestBody @Valid GenerateHintsRequest req) {
        return service.generateReplenishmentHints(req.tenantId,
                req.period != null ? req.period : "WEEKLY");
    }

    @GetMapping("/replenishment")
    public List<AiReplenishmentHintEntity> getHints(@RequestParam String tenantId) {
        return service.getHints(tenantId);
    }

    @GetMapping("/replenishment/pending")
    public List<AiReplenishmentHintEntity> getPendingHints(@RequestParam String tenantId) {
        return service.getPendingHints(tenantId);
    }

    @PostMapping("/replenishment/{hintId}/dismiss")
    public AiReplenishmentHintEntity dismissHint(@PathVariable String hintId) {
        return service.dismissHint(hintId);
    }

    @Data
    static class GenerateHintsRequest {
        @NotBlank String tenantId;
        String period;
    }

    // ── BC-1903: Demand Forecasting ──────────────────────────────────────────

    @PostMapping("/forecast/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiDemandForecastEntity> generateForecast(@RequestBody @Valid GenerateForecastRequest req) {
        return service.generateDemandForecast(req.tenantId,
                req.forecastDays != null ? req.forecastDays : 7);
    }

    @GetMapping("/forecast")
    public List<AiDemandForecastEntity> getForecasts(@RequestParam String tenantId) {
        return service.getForecasts(tenantId);
    }

    @Data
    static class GenerateForecastRequest {
        @NotBlank String tenantId;
        Integer forecastDays;
    }

    // ── BC-1902: Production Suggestions ──────────────────────────────────────

    @PostMapping("/production/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiProductionSuggestionEntity> generateProductionSuggestions(
            @RequestBody @Valid GenerateProductionRequest req) {
        LocalDate date = req.planDate != null ? req.planDate : LocalDate.now();
        return service.generateProductionSuggestions(req.tenantId, date);
    }

    @GetMapping("/production")
    public List<AiProductionSuggestionEntity> getSuggestions(
            @RequestParam String tenantId,
            @RequestParam(required = false) LocalDate planDate) {
        if (planDate != null) {
            return service.getSuggestions(tenantId, planDate);
        }
        return service.getAllSuggestions(tenantId);
    }

    @Data
    static class GenerateProductionRequest {
        @NotBlank String tenantId;
        LocalDate planDate;
    }

    // ── D3.1: Advanced Forecasting ───────────────────────────────────────────

    private final AiAdvancedForecastService advancedForecastService;
    private final QualityPredictionService qualityPredictionService;

    @PostMapping("/forecast/advanced/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiDemandForecastEntity> generateAdvancedForecast(@RequestBody @Valid GenerateForecastRequest req) {
        return advancedForecastService.generateAdvancedForecast(req.tenantId,
                req.forecastDays != null ? req.forecastDays : 7);
    }

    // ── D3.4: Quality Predictions ────────────────────────────────────────────

    @PostMapping("/quality/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AiQualityPredictionEntity> generateQualityPredictions(@RequestBody @Valid QualityRequest req) {
        return qualityPredictionService.generatePredictions(req.tenantId);
    }

    @GetMapping("/quality")
    public List<AiQualityPredictionEntity> getQualityPredictions(@RequestParam String tenantId) {
        return qualityPredictionService.getPredictions(tenantId);
    }

    @GetMapping("/quality/high-risk")
    public List<AiQualityPredictionEntity> getHighRiskPredictions(@RequestParam String tenantId) {
        return qualityPredictionService.getHighRiskPredictions(tenantId);
    }

    @PostMapping("/quality/{predictionId}/dismiss")
    public AiQualityPredictionEntity dismissQualityPrediction(@PathVariable String predictionId) {
        return qualityPredictionService.dismissPrediction(predictionId);
    }

    @Data
    static class QualityRequest { @NotBlank String tenantId; }
}
