package com.breadcost.customers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
}
