package com.breadcost.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRunRepository extends JpaRepository<DeliveryRunEntity, String> {
    List<DeliveryRunEntity> findByTenantId(String tenantId);
    Optional<DeliveryRunEntity> findByTenantIdAndRunId(String tenantId, String runId);
}
