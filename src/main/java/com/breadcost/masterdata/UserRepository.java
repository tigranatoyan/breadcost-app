package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByTenantIdAndUsername(String tenantId, String username);
    List<UserEntity> findByTenantId(String tenantId);
    Optional<UserEntity> findByUsername(String username); // for auth (cross-tenant username lookup)
}
