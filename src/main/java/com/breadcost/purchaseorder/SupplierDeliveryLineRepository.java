package com.breadcost.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierDeliveryLineRepository extends JpaRepository<SupplierDeliveryLineEntity, String> {
    List<SupplierDeliveryLineEntity> findByDeliveryId(String deliveryId);
}
