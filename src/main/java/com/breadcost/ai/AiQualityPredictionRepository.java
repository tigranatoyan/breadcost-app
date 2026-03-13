package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiQualityPredictionRepository extends JpaRepository<AiQualityPredictionEntity, String> {
    List<AiQualityPredictionEntity> findByTenantId(String tenantId);
    List<AiQualityPredictionEntity> findByTenantIdAndStatus(String tenantId, String status);
    List<AiQualityPredictionEntity> findByTenantIdAndProductId(String tenantId, String productId);
    List<AiQualityPredictionEntity> findByTenantIdAndRiskLevel(String tenantId, String riskLevel);
}
