package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity, String> {
    List<SaleEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Query("SELECT s FROM SaleEntity s WHERE s.tenantId = :tenantId AND s.completedAt >= :from AND s.completedAt < :to ORDER BY s.completedAt DESC")
    List<SaleEntity> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
