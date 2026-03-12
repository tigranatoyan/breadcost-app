package com.breadcost.masterdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.breadcost.subscription.SubscriptionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

    public List<UserEntity> getUsers(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public Optional<UserEntity> getUserById(String tenantId, String userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId));
    }

    public UserEntity createUser(String tenantId, String username, String password,
                                  String displayName, String roles, String departmentId) {
        // BC-3102: Enforce maxUsers limit
        int maxUsers = subscriptionService.getMaxUsers(tenantId);
        if (maxUsers > 0) {
            long activeCount = userRepository.findByTenantId(tenantId).stream()
                    .filter(UserEntity::isActive).count();
            if (activeCount >= maxUsers) {
                throw new IllegalStateException(
                        "User limit reached (" + maxUsers + "). Upgrade your plan to add more users.");
            }
        }

        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .roles(roles)
                .departmentId(departmentId)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    public UserEntity updateUser(String tenantId, String userId, String displayName,
                                  String roles, String departmentId, Boolean active) {
        UserEntity user = userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (displayName != null) user.setDisplayName(displayName);
        if (roles != null) user.setRoles(roles);
        if (departmentId != null) user.setDepartmentId(departmentId);
        if (active != null) user.setActive(active);
        return userRepository.save(user);
    }

    public UserEntity resetPassword(String tenantId, String userId, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /** Find by username globally (for login — username must be unique per tenant, but login context provides tenantId or we do global lookup) */
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean checkPassword(UserEntity user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
