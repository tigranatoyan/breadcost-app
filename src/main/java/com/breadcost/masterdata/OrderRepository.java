package com.breadcost.masterdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByTenantId(String tenantId);
    List<OrderEntity> findByTenantIdAndStatus(String tenantId, String status);
    List<OrderEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
    Page<OrderEntity> findByTenantId(String tenantId, Pageable pageable);
}
