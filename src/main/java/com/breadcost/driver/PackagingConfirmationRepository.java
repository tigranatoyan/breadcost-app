package com.breadcost.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackagingConfirmationRepository extends JpaRepository<PackagingConfirmationEntity, String> {

    Optional<PackagingConfirmationEntity> findByTenantIdAndRunId(String tenantId, String runId);

    List<PackagingConfirmationEntity> findByTenantId(String tenantId);
}
