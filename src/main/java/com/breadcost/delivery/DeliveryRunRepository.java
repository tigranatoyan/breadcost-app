package com.breadcost.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeliveryRunRepository extends JpaRepository<DeliveryRunEntity, String> {
    List<DeliveryRunEntity> findByTenantId(String tenantId);
    Optional<DeliveryRunEntity> findByTenantIdAndRunId(String tenantId, String runId);

    @Query("SELECT COALESCE(MAX(r.runNumber), 0) FROM DeliveryRunEntity r WHERE r.tenantId = :tenantId")
    int maxRunNumber(String tenantId);
}
