package com.breadcost.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiReplenishmentHintRepository extends JpaRepository<AiReplenishmentHintEntity, String> {

    List<AiReplenishmentHintEntity> findByTenantId(String tenantId);

    List<AiReplenishmentHintEntity> findByTenantIdAndStatus(String tenantId, String status);

    List<AiReplenishmentHintEntity> findByTenantIdAndItemId(String tenantId, String itemId);
}
