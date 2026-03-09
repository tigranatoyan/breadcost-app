package com.breadcost.mobile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PushNotificationRepository extends JpaRepository<PushNotificationEntity, String> {
    List<PushNotificationEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
    List<PushNotificationEntity> findByTenantIdAndCustomerIdAndStatus(String tenantId, String customerId, String status);
}
