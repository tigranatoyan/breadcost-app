package com.breadcost.api;

import com.breadcost.exchangerate.ExchangeRateEntity;
import com.breadcost.exchangerate.ExchangeRateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exchange rate endpoints — BC-2201 (FR-6.7, FR-9.7)
 */
@Tag(name = "Exchange Rates", description = "Currency exchange rate management")
@RestController
@RequestMapping("/v3/exchange-rates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin','Manager','FinanceUser')")
public class ExchangeRateController {

    private final ExchangeRateService service;

    // ── Manual rate entry ─────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRateEntity setRate(@RequestBody @Valid SetRateRequest req) {
        return service.setRate(req.tenantId, req.baseCurrency, req.currencyCode,
                req.rate, req.rateDate);
    }

    @Data
    static class SetRateRequest {
        @NotBlank String tenantId;
        @NotBlank String baseCurrency;
        @NotBlank String currencyCode;
        @NotNull  Double rate;
        @NotNull  LocalDate rateDate;
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    @GetMapping
    public List<ExchangeRateEntity> listRates(@RequestParam String tenantId) {
        return service.listRates(tenantId);
    }

    @GetMapping("/lookup")
    public ExchangeRateEntity getRate(@RequestParam String tenantId,
                                       @RequestParam String currencyCode,
                                       @RequestParam LocalDate date) {
        return service.getRate(tenantId, currencyCode, date);
    }

    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam String tenantId,
                                        @RequestParam String from,
                                        @RequestParam String to,
                                        @RequestParam double amount,
                                        @RequestParam LocalDate date) {
        double result = service.convert(tenantId, from, to, amount, date);
        return Map.of("from", from, "to", to, "amount", amount,
                "converted", result, "date", date.toString());
    }

    // ── API fetch ─────────────────────────────────────────────────────────────

    @PostMapping("/fetch")
    public List<ExchangeRateEntity> fetchFromApi(@RequestBody @Valid FetchRequest req) {
        return service.fetchRatesFromApi(req.tenantId, req.baseCurrency, req.targetCurrencies);
    }

    @Data
    static class FetchRequest {
        @NotBlank String tenantId;
        @NotBlank String baseCurrency;
        @NotNull  List<String> targetCurrencies;
    }
}
