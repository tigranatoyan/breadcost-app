package com.breadcost.unit.service;

import com.breadcost.exchangerate.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private ExchangeRateRepository repo;
    @Mock private RestClient.Builder restClientBuilder;
    @InjectMocks private ExchangeRateService svc;

    private static final LocalDate TODAY = LocalDate.of(2025, 1, 15);

    // ── setRate ──────────────────────────────────────────────────────────────

    @Test
    void setRate_newRate_createsEntity() {
        when(repo.findByTenantIdAndBaseCurrencyAndCurrencyCodeAndRateDate(
                "t1", "USD", "AMD", TODAY)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var rate = svc.setRate("t1", "USD", "AMD", 385.0, TODAY);

        assertEquals("t1", rate.getTenantId());
        assertEquals("AMD", rate.getCurrencyCode());
        assertEquals(385.0, rate.getRate());
        assertEquals("MANUAL", rate.getSource());
    }

    @Test
    void setRate_existing_updatesRate() {
        var existing = ExchangeRateEntity.builder()
                .rateId("r1").tenantId("t1").baseCurrency("USD")
                .currencyCode("AMD").rate(380.0).rateDate(TODAY).source("API").build();
        when(repo.findByTenantIdAndBaseCurrencyAndCurrencyCodeAndRateDate(
                "t1", "USD", "AMD", TODAY)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var rate = svc.setRate("t1", "USD", "AMD", 385.0, TODAY);

        assertEquals(385.0, rate.getRate());
        assertEquals("MANUAL", rate.getSource());
    }

    // ── getRate ──────────────────────────────────────────────────────────────

    @Test
    void getRate_exactDate_found() {
        var entity = ExchangeRateEntity.builder()
                .rateId("r1").tenantId("t1").currencyCode("AMD").rate(385.0).build();
        when(repo.findByTenantIdAndCurrencyCodeAndRateDate("t1", "AMD", TODAY))
                .thenReturn(Optional.of(entity));

        var rate = svc.getRate("t1", "AMD", TODAY);

        assertEquals(385.0, rate.getRate());
    }

    @Test
    void getRate_fallsBackToLatest() {
        when(repo.findByTenantIdAndCurrencyCodeAndRateDate("t1", "AMD", TODAY))
                .thenReturn(Optional.empty());
        var fallback = ExchangeRateEntity.builder()
                .rateId("r1").tenantId("t1").currencyCode("AMD").rate(380.0).build();
        when(repo.findTopByTenantIdAndCurrencyCodeOrderByRateDateDesc("t1", "AMD"))
                .thenReturn(Optional.of(fallback));

        var rate = svc.getRate("t1", "AMD", TODAY);

        assertEquals(380.0, rate.getRate());
    }

    @Test
    void getRate_noRateAtAll_throws() {
        when(repo.findByTenantIdAndCurrencyCodeAndRateDate("t1", "XYZ", TODAY))
                .thenReturn(Optional.empty());
        when(repo.findTopByTenantIdAndCurrencyCodeOrderByRateDateDesc("t1", "XYZ"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.getRate("t1", "XYZ", TODAY));
    }

    // ── convert ──────────────────────────────────────────────────────────────

    @Test
    void convert_sameCurrency_returnsAmount() {
        assertEquals(100.0, svc.convert("t1", "USD", "USD", 100.0, TODAY));
    }

    @Test
    void convert_differentCurrency_multiplies() {
        var entity = ExchangeRateEntity.builder()
                .rateId("r1").tenantId("t1").currencyCode("AMD").rate(385.0).build();
        when(repo.findByTenantIdAndCurrencyCodeAndRateDate("t1", "AMD", TODAY))
                .thenReturn(Optional.of(entity));

        double result = svc.convert("t1", "USD", "AMD", 10.0, TODAY);

        assertEquals(3850.0, result);
    }
}
