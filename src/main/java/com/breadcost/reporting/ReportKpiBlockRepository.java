package com.breadcost.reporting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportKpiBlockRepository extends JpaRepository<ReportKpiBlockEntity, String> {
    List<ReportKpiBlockEntity> findByActive(boolean active);
    List<ReportKpiBlockEntity> findByCategory(ReportKpiBlockEntity.KpiCategory category);
    Optional<ReportKpiBlockEntity> findByBlockKey(String blockKey);
}
