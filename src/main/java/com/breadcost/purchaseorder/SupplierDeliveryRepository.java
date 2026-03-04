package com.breadcost.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierDeliveryRepository extends JpaRepository<SupplierDeliveryEntity, String> {
    List<SupplierDeliveryEntity> findByTenantId(String tenantId);
    Optional<SupplierDeliveryEntity> findByTenantIdAndDeliveryId(String tenantId, String deliveryId);
    List<SupplierDeliveryEntity> findByTenantIdAndPoId(String tenantId, String poId);
}
