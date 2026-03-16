package com.breadcost.functional;

import com.breadcost.ai.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("R3 :: BC-2001/2002 — AI Pricing & Anomaly")
class AiPricingAnomalyTest extends FunctionalTestBase {

    @Autowired AiPricingSuggestionRepository pricingRepo;
    @Autowired AiAnomalyAlertRepository anomalyRepo;

    // ── BC-2001: Pricing Suggestions ─────────────────────────────────────────

    @Test @DisplayName("BC-2001 ✓ Generate pricing suggestions returns 201")
    void generatePricing_201() throws Exception {
        POST("/v3/ai/pricing/generate",
                Map.of("tenantId", TENANT),
                bearer("admin1"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("BC-2001 ✓ Get pricing suggestions returns 200")
    void getPricing_200() throws Exception {
        GET("/v3/ai/pricing?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-2001 ✓ Get pending pricing returns 200")
    void getPendingPricing_200() throws Exception {
        GET("/v3/ai/pricing/pending?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-2001 ✓ Dismiss pricing suggestion updates status")
    void dismissPricing_updatesStatus() throws Exception {
        AiPricingSuggestionEntity sug = AiPricingSuggestionEntity.builder()
                .suggestionId("ps-dismiss-1")
                .tenantId(TENANT)
                .productId("prod-1")
                .productName("White Bread")
                .currentPrice(new BigDecimal("1000"))
                .suggestedPrice(new BigDecimal("970"))
                .changePct(new BigDecimal("-3"))
                .reason("Volume discount")
                .build();
        pricingRepo.save(sug);

        POST_noBody("/v3/ai/pricing/ps-dismiss-1/dismiss", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test @DisplayName("BC-2001 ✓ Accept pricing suggestion updates status")
    void acceptPricing_updatesStatus() throws Exception {
        AiPricingSuggestionEntity sug = AiPricingSuggestionEntity.builder()
                .suggestionId("ps-accept-1")
                .tenantId(TENANT)
                .productId("prod-1")
                .currentPrice(new BigDecimal("1000"))
                .suggestedPrice(new BigDecimal("970"))
                .changePct(new BigDecimal("-3"))
                .reason("Volume discount")
                .build();
        pricingRepo.save(sug);

        POST_noBody("/v3/ai/pricing/ps-accept-1/accept", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test @DisplayName("BC-2001 ✓ Dismiss non-existent suggestion returns 404")
    void dismissPricing_notFound_404() throws Exception {
        POST_noBody("/v3/ai/pricing/no-such/dismiss", bearer("admin1"))
                .andExpect(status().isNotFound());
    }

    // ── BC-2002: Anomaly Alerts ──────────────────────────────────────────────

    @Test @DisplayName("BC-2002 ✓ Generate anomaly alerts returns 201")
    void generateAlerts_201() throws Exception {
        POST("/v3/ai/anomalies/generate",
                Map.of("tenantId", TENANT),
                bearer("admin1"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("BC-2002 ✓ Get alerts returns 200")
    void getAlerts_200() throws Exception {
        GET("/v3/ai/anomalies?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-2002 ✓ Get active alerts returns 200")
    void getActiveAlerts_200() throws Exception {
        GET("/v3/ai/anomalies/active?tenantId=" + TENANT,
                bearer("admin1"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("BC-2002 ✓ Acknowledge alert updates status")
    void acknowledgeAlert_updatesStatus() throws Exception {
        AiAnomalyAlertEntity alert = AiAnomalyAlertEntity.builder()
                .alertId("alert-ack-1")
                .tenantId(TENANT)
                .alertType("REVENUE_DROP")
                .severity("WARNING")
                .metricName("Weekly Revenue")
                .explanation("Revenue drop detected")
                .suggestedAction("Review trends")
                .build();
        anomalyRepo.save(alert);

        POST_noBody("/v3/ai/anomalies/alert-ack-1/acknowledge", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test @DisplayName("BC-2002 ✓ Dismiss alert updates status")
    void dismissAlert_updatesStatus() throws Exception {
        AiAnomalyAlertEntity alert = AiAnomalyAlertEntity.builder()
                .alertId("alert-dismiss-1")
                .tenantId(TENANT)
                .alertType("ORDER_VOLUME_SPIKE")
                .severity("INFO")
                .explanation("Volume spike")
                .build();
        anomalyRepo.save(alert);

        POST_noBody("/v3/ai/anomalies/alert-dismiss-1/dismiss", bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test @DisplayName("BC-2002 ✓ Dismiss non-existent alert returns 404")
    void dismissAlert_notFound_404() throws Exception {
        POST_noBody("/v3/ai/anomalies/no-such/dismiss", bearer("admin1"))
                .andExpect(status().isNotFound());
    }
}
