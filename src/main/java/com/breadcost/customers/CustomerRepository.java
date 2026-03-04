package com.breadcost.customers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {

    Optional<CustomerEntity> findByTenantIdAndEmail(String tenantId, String email);

    List<CustomerEntity> findByTenantId(String tenantId);
}
