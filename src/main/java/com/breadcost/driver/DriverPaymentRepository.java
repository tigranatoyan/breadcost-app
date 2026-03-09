package com.breadcost.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverPaymentRepository extends JpaRepository<DriverPaymentEntity, String> {

    List<DriverPaymentEntity> findByTenantIdAndOrderId(String tenantId, String orderId);

    List<DriverPaymentEntity> findBySessionId(String sessionId);

    List<DriverPaymentEntity> findByTenantId(String tenantId);
}
