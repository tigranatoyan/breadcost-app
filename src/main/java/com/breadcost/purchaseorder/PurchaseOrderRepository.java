package com.breadcost.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, String> {

    List<PurchaseOrderEntity> findByTenantId(String tenantId);

    List<PurchaseOrderEntity> findByTenantIdAndSupplierId(String tenantId, String supplierId);

    List<PurchaseOrderEntity> findByTenantIdAndStatus(String tenantId, PurchaseOrderEntity.PoStatus status);

    Optional<PurchaseOrderEntity> findByTenantIdAndPoId(String tenantId, String poId);
}
