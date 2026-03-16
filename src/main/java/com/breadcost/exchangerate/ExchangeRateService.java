package com.breadcost.exchangerate;

import com.breadcost.masterdata.TenantConfigEntity;
import com.breadcost.masterdata.TenantConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.*;

/**
 * Exchange rate service — BC-2201 (FR-6.7, FR-9.7)
 *
 * Supports:
 * - Manual rate entry per currency per date
 * - Automatic daily fetch from exchangerate-api.com (free tier)
 * - Rate lookup with fallback to latest available
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final TenantConfigRepository tenantConfigRepo;
    private final RestClient.Builder restClientBuilder;

    private static final String API_URL = "https://open.er-api.com/v6/latest/";

    // ── Manual rate entry ─────────────────────────────────────────────────────

    @Transactional
    public ExchangeRateEntity setRate(String tenantId, String baseCurrency,
                                       String currencyCode, double rate, LocalDate rateDate) {
        Optional<ExchangeRateEntity> existing = repository
                .findByTenantIdAndBaseCurrencyAndCurrencyCodeAndRateDate(
                        tenantId, baseCurrency, currencyCode, rateDate);

        if (existing.isPresent()) {
            ExchangeRateEntity entity = existing.get();
            entity.setRate(rate);
            entity.setSource("MANUAL");
            return repository.save(entity);
        }

        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .rateId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .baseCurrency(baseCurrency)
                .currencyCode(currencyCode)
                .rate(rate)
                .rateDate(rateDate)
                .source("MANUAL")
                .build();

        log.info("Setting exchange rate: tenantId={} {}→{} = {} on {}",
                tenantId, baseCurrency, currencyCode, rate, rateDate);
        return repository.save(entity);
    }

    // ── Rate lookup ───────────────────────────────────────────────────────────

    public ExchangeRateEntity getRate(String tenantId, String currencyCode, LocalDate date) {
        return repository.findByTenantIdAndCurrencyCodeAndRateDate(tenantId, currencyCode, date)
                .orElseGet(() -> repository.findTopByTenantIdAndCurrencyCodeOrderByRateDateDesc(
                        tenantId, currencyCode)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No exchange rate found for " + currencyCode)));
    }

    public double convert(String tenantId, String fromCurrency, String toCurrency,
                          double amount, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) return amount;
        ExchangeRateEntity rate = getRate(tenantId, toCurrency, date);
        return amount * rate.getRate();
    }

    public List<ExchangeRateEntity> listRates(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public List<ExchangeRateEntity> listRatesByDateRange(String tenantId,
                                                          LocalDate from, LocalDate to) {
        return repository.findByTenantIdAndRateDateBetween(tenantId, from, to);
    }

    // ── API fetch ─────────────────────────────────────────────────────────────

    /**
     * Fetch today's rates from the free exchange rate API for the given base currency.
     * Stores results with source=API. Idempotent — overwrites existing API rates for today.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public List<ExchangeRateEntity> fetchRatesFromApi(String tenantId, String baseCurrency,
                                                       List<String> targetCurrencies) {
        return doFetchRatesFromApi(tenantId, baseCurrency, targetCurrencies);
    }

    @SuppressWarnings("unchecked")
    private List<ExchangeRateEntity> doFetchRatesFromApi(String tenantId, String baseCurrency,
                                                          List<String> targetCurrencies) {
        RestClient client = restClientBuilder.build();
        Map<String, Object> response;
        try {
            response = client.get()
                    .uri(API_URL + baseCurrency)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates from API: {}", e.getMessage());
            throw new IllegalStateException("Exchange rate API unavailable", e);
        }

        if (response == null || !"success".equals(response.get("result"))) {
            throw new IllegalStateException("Exchange rate API returned error");
        }

        Map<String, Number> rates = (Map<String, Number>) response.get("rates");
        if (rates == null) {
            throw new IllegalStateException("No rates in API response");
        }

        LocalDate today = LocalDate.now();
        List<ExchangeRateEntity> saved = new ArrayList<>();

        for (String currency : targetCurrencies) {
            Number apiRate = rates.get(currency);
            if (apiRate == null) {
                log.warn("Currency {} not found in API response", currency);
                continue;
            }

            Optional<ExchangeRateEntity> existing = repository
                    .findByTenantIdAndBaseCurrencyAndCurrencyCodeAndRateDate(
                            tenantId, baseCurrency, currency, today);

            ExchangeRateEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
                entity.setRate(apiRate.doubleValue());
                entity.setSource("API");
            } else {
                entity = ExchangeRateEntity.builder()
                        .rateId(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .baseCurrency(baseCurrency)
                        .currencyCode(currency)
                        .rate(apiRate.doubleValue())
                        .rateDate(today)
                        .source("API")
                        .build();
            }
            saved.add(repository.save(entity));
        }

        log.info("Fetched {} exchange rates from API for tenantId={} base={}",
                saved.size(), tenantId, baseCurrency);
        return saved;
    }

    /**
     * Refresh exchange rates for all tenants. Each tenant's mainCurrency is used as base.
     * Returns total number of rates refreshed across all tenants.
     */
    @Transactional
    public int refreshAllTenantRates() {
        List<String> defaultTargets = List.of("USD", "EUR", "RUB", "GEL", "AMD");
        int total = 0;
        for (TenantConfigEntity config : tenantConfigRepo.findAll()) {
            try {
                String base = config.getMainCurrency() != null ? config.getMainCurrency() : "AMD";
                List<ExchangeRateEntity> fetched = doFetchRatesFromApi(config.getTenantId(), base, defaultTargets);
                total += fetched.size();
            } catch (Exception e) {
                log.error("Failed to refresh rates for tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
        return total;
    }
}
