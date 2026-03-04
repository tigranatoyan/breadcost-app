package com.breadcost.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccountEntity, String> {
    Optional<LoyaltyAccountEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
}
