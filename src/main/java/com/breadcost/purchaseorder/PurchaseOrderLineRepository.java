package com.breadcost.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, String> {
    List<PurchaseOrderLineEntity> findByPoId(String poId);
    void deleteByPoId(String poId);
}
