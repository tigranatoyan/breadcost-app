package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AiDemandForecastRepository extends JpaRepository<AiDemandForecastEntity, String> {

    List<AiDemandForecastEntity> findByTenantId(String tenantId);

    List<AiDemandForecastEntity> findByTenantIdAndProductId(String tenantId, String productId);

    List<AiDemandForecastEntity> findByTenantIdAndPeriodStartGreaterThanEqual(String tenantId, LocalDate from);
}
