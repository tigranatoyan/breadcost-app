package com.breadcost.exchangerate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, String> {

    List<ExchangeRateEntity> findByTenantId(String tenantId);

    List<ExchangeRateEntity> findByTenantIdAndCurrencyCode(String tenantId, String currencyCode);

    Optional<ExchangeRateEntity> findByTenantIdAndCurrencyCodeAndRateDate(
            String tenantId, String currencyCode, LocalDate rateDate);

    Optional<ExchangeRateEntity> findByTenantIdAndBaseCurrencyAndCurrencyCodeAndRateDate(
            String tenantId, String baseCurrency, String currencyCode, LocalDate rateDate);

    /** Latest rate for a currency — useful for current conversion. */
    Optional<ExchangeRateEntity> findTopByTenantIdAndCurrencyCodeOrderByRateDateDesc(
            String tenantId, String currencyCode);

    List<ExchangeRateEntity> findByTenantIdAndRateDateBetween(
            String tenantId, LocalDate from, LocalDate to);
}
