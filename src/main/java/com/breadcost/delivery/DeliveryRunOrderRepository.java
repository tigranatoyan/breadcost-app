package com.breadcost.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRunOrderRepository extends JpaRepository<DeliveryRunOrderEntity, String> {
    List<DeliveryRunOrderEntity> findByRunId(String runId);
    Optional<DeliveryRunOrderEntity> findByRunIdAndOrderId(String runId, String orderId);
    List<DeliveryRunOrderEntity> findByTenantIdAndOrderId(String tenantId, String orderId);
}
