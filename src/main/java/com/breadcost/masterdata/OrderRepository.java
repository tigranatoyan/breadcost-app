package com.breadcost.masterdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByTenantId(String tenantId);
    List<OrderEntity> findByTenantIdAndStatus(String tenantId, String status);
    List<OrderEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
    Page<OrderEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<OrderEntity> findTopByTenantIdOrderByOrderNumberDesc(String tenantId);

    @Query("SELECT DISTINCT ol.order FROM OrderLineEntity ol WHERE ol.orderLineId IN :lineIds AND ol.tenantId = :tenantId")
    List<OrderEntity> findByTenantIdAndOrderLineIds(@Param("tenantId") String tenantId, @Param("lineIds") Collection<String> lineIds);
}
