package com.breadcost.customers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerDiscountRuleRepository extends JpaRepository<CustomerDiscountRuleEntity, String> {
    List<CustomerDiscountRuleEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
    List<CustomerDiscountRuleEntity> findByTenantIdAndCustomerIdAndActive(String tenantId, String customerId, boolean active);
    List<CustomerDiscountRuleEntity> findByTenantIdAndCustomerIdAndItemId(String tenantId, String customerId, String itemId);
}
