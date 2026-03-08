package com.breadcost.reporting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomReportRepository extends JpaRepository<CustomReportEntity, String> {
    List<CustomReportEntity> findByTenantId(String tenantId);
    List<CustomReportEntity> findByTenantIdAndActive(String tenantId, boolean active);
    Optional<CustomReportEntity> findByTenantIdAndReportId(String tenantId, String reportId);
}
