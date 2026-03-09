package com.breadcost.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BC-2201: Exchange Rate API integration tests (FR-6.7, FR-9.7)
 */
@DisplayName("R3 :: BC-2201 — Exchange Rate API")
class ExchangeRateTest extends FunctionalTestBase {

    private static final String BASE = "/v3/exchange-rates";

    @Test
    @DisplayName("BC-2201 ✓ Set manual exchange rate returns 201")
    void setRate_returns201() throws Exception {
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "AMD",
                "rate", 385.50,
                "rateDate", LocalDate.now().toString()
        ), bearer("admin1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rateId").isNotEmpty())
                .andExpect(jsonPath("$.currencyCode").value("AMD"))
                .andExpect(jsonPath("$.rate").value(385.50));
    }

    @Test
    @DisplayName("BC-2201 ✓ List rates returns created rate")
    void listRates_returnsRate() throws Exception {
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "EUR",
                "rate", 0.92,
                "rateDate", LocalDate.now().toString()
        ), bearer("admin1")).andExpect(status().isCreated());

        GET(BASE + "?tenantId=" + TENANT, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].currencyCode", hasItem("EUR")));
    }

    @Test
    @DisplayName("BC-2201 ✓ Lookup specific rate by currency and date")
    void lookupRate_returnsExact() throws Exception {
        String today = LocalDate.now().toString();
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "GBP",
                "rate", 0.79,
                "rateDate", today
        ), bearer("admin1")).andExpect(status().isCreated());

        GET(BASE + "/lookup?tenantId=" + TENANT +
                "&currencyCode=GBP&date=" + today, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(0.79));
    }

    @Test
    @DisplayName("BC-2201 ✓ Convert between currencies")
    void convert_returnsResult() throws Exception {
        String today = LocalDate.now().toString();
        // Set USD→AMD rate
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "AMD",
                "rate", 400.0,
                "rateDate", today
        ), bearer("admin1")).andExpect(status().isCreated());

        // Set USD→EUR rate
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "EUR",
                "rate", 0.90,
                "rateDate", today
        ), bearer("admin1")).andExpect(status().isCreated());

        GET(BASE + "/convert?tenantId=" + TENANT +
                "&from=AMD&to=EUR&amount=40000&date=" + today, bearer("admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("AMD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.converted").isNumber());
    }

    @Test
    @DisplayName("BC-2201 ✓ Upsert rate — same currency+date overwrites")
    void upsertRate_overwritesSameDate() throws Exception {
        String today = LocalDate.now().toString();
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "RUB",
                "rate", 90.0,
                "rateDate", today
        ), bearer("admin1")).andExpect(status().isCreated());

        // Second set should overwrite
        POST(BASE, Map.of(
                "tenantId", TENANT,
                "baseCurrency", "USD",
                "currencyCode", "RUB",
                "rate", 92.5,
                "rateDate", today
        ), bearer("admin1")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rate").value(92.5));
    }
}
