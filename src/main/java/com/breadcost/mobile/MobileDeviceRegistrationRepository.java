package com.breadcost.mobile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MobileDeviceRegistrationRepository extends JpaRepository<MobileDeviceRegistrationEntity, String> {
    List<MobileDeviceRegistrationEntity> findByTenantIdAndCustomerIdAndActiveTrue(String tenantId, String customerId);
    Optional<MobileDeviceRegistrationEntity> findByTenantIdAndDeviceToken(String tenantId, String deviceToken);
    List<MobileDeviceRegistrationEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
}
