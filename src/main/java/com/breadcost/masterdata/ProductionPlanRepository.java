package com.breadcost.masterdata;

import com.breadcost.domain.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlanEntity, String> {
    List<ProductionPlanEntity> findByTenantId(String tenantId);
    List<ProductionPlanEntity> findByTenantIdOrderByPlanDateDesc(String tenantId);
    List<ProductionPlanEntity> findByTenantIdAndPlanDateOrderByPlanDateDesc(String tenantId, LocalDate planDate);
    List<ProductionPlanEntity> findByTenantIdAndStatusOrderByPlanDateDesc(String tenantId, ProductionPlan.Status status);
    Optional<ProductionPlanEntity> findByTenantIdAndPlanIdOrderByCreatedAtDesc(String tenantId, String planId);
}
