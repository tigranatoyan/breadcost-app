package com.breadcost.functional;

import com.breadcost.ai.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("R3 :: BC-1901/1902/1903 — AI Suggestions")
class AiSuggestionTest extends FunctionalTestBase {

    @Autowired AiReplenishmentHintRepository hintRepo;
    @Autowired AiDemandForecastRepository forecastRepo;
    @Autowired AiProductionSuggestionRepository prodSugRepo;

    // ── BC-1901: Replenishment Hints ─────────────────────────────────────────

    @Test @DisplayName("BC-1901 ✓ Generate replenishment hints returns 201")
    void generateReplenishmentHints_201() throws Exception {
        POST("/v3/ai/suggestions/replenishment/generate",
                Map.of("tenantId", TENANT, "period", "WEEKLY"),
                bearer("admin1"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("BC-1901 ✓ Get hints returns 200")
    void getHints_200() throws Exception {
        GET("/v3/ai/suggestions/replenishment?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-1901 ✓ Get pending hints returns 200")
    void getPendingHints_200() throws Exception {
        GET("/v3/ai/suggestions/replenishment/pending?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-1901 ✓ Dismiss hint updates status")
    void dismissHint_updatesStatus() throws Exception {
        AiReplenishmentHintEntity hint = AiReplenishmentHintEntity.builder()
                .hintId("hint-dismiss-1")
                .tenantId(TENANT)
                .itemId("item-1")
                .currentQty(10)
                .avgDailyUse(2)
                .daysLeft(5.0)
                .suggestedQty(20)
                .unit("kg")
                .period("WEEKLY")
                .build();
        hintRepo.save(hint);

        POST_noBody("/v3/ai/suggestions/replenishment/hint-dismiss-1/dismiss",
                bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test @DisplayName("BC-1901 ✓ Dismiss non-existent hint returns 404")
    void dismissHint_notFound_404() throws Exception {
        POST_noBody("/v3/ai/suggestions/replenishment/no-such-hint/dismiss",
                bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── BC-1903: Demand Forecast ─────────────────────────────────────────────

    @Test @DisplayName("BC-1903 ✓ Generate demand forecast returns 201")
    void generateForecast_201() throws Exception {
        POST("/v3/ai/suggestions/forecast/generate",
                Map.of("tenantId", TENANT, "forecastDays", 7),
                bearer("admin1"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("BC-1903 ✓ Get forecasts returns 200")
    void getForecasts_200() throws Exception {
        GET("/v3/ai/suggestions/forecast?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    // ── BC-1902: Production Suggestions ──────────────────────────────────────

    @Test @DisplayName("BC-1902 ✓ Generate production suggestions returns 201")
    void generateProdSuggestions_201() throws Exception {
        POST("/v3/ai/suggestions/production/generate",
                Map.of("tenantId", TENANT),
                bearer("admin1"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("BC-1902 ✓ Get production suggestions returns 200")
    void getProdSuggestions_200() throws Exception {
        GET("/v3/ai/suggestions/production?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-1902 ✓ Get production suggestions by date returns 200")
    void getProdSuggestions_byDate_200() throws Exception {
        GET("/v3/ai/suggestions/production?tenantId=" + TENANT + "&planDate=2025-01-15",
                bearer("admin1"))
                .andExpect(status().isOk());
    }
}
