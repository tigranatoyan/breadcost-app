package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiAnomalyAlertRepository extends JpaRepository<AiAnomalyAlertEntity, String> {
    List<AiAnomalyAlertEntity> findByTenantId(String tenantId);
    List<AiAnomalyAlertEntity> findByTenantIdAndStatus(String tenantId, String status);
    List<AiAnomalyAlertEntity> findByTenantIdAndAlertType(String tenantId, String alertType);
}
