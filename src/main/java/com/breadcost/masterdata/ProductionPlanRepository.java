package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlanEntity, String> {
    List<ProductionPlanEntity> findByTenantId(String tenantId);
    List<ProductionPlanEntity> findByTenantIdAndPlanDate(String tenantId, LocalDate planDate);
    List<ProductionPlanEntity> findByTenantIdAndStatus(String tenantId, ProductionPlan.Status status);
    Optional<ProductionPlanEntity> findByTenantIdAndPlanIdOrderByCreatedAtDesc(String tenantId, String planId);
}
