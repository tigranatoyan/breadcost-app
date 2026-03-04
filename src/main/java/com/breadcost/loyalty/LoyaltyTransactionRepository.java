package com.breadcost.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransactionEntity, String> {
    List<LoyaltyTransactionEntity> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            String tenantId, String customerId);
}
