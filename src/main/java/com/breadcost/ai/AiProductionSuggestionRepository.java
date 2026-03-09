package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AiProductionSuggestionRepository extends JpaRepository<AiProductionSuggestionEntity, String> {

    List<AiProductionSuggestionEntity> findByTenantId(String tenantId);

    List<AiProductionSuggestionEntity> findByTenantIdAndPlanDate(String tenantId, LocalDate planDate);

    List<AiProductionSuggestionEntity> findByTenantIdAndStatus(String tenantId, String status);
}
