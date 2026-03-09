package com.breadcost.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverSessionRepository extends JpaRepository<DriverSessionEntity, String> {

    List<DriverSessionEntity> findByTenantId(String tenantId);

    List<DriverSessionEntity> findByTenantIdAndStatus(String tenantId, String status);

    Optional<DriverSessionEntity> findByTenantIdAndDriverIdAndStatus(String tenantId, String driverId, String status);

    Optional<DriverSessionEntity> findByTenantIdAndRunId(String tenantId, String runId);
}
