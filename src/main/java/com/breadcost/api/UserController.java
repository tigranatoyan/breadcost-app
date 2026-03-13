package com.breadcost.api;

import com.breadcost.masterdata.UserEntity;
import com.breadcost.masterdata.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User management — FR-11.4
 * POST/PUT /v1/users
 */
@Tag(name = "Users", description = "User CRUD, roles, and access management")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @Data
    public static class CreateUserRequest {
        @NotBlank private String tenantId;
        @NotBlank private String username;
        @NotBlank private String password;
        private String displayName;
        /** Comma-separated: Admin,ProductionUser etc. */
        private String roles;
        private String departmentId;
    }

    @Data
    public static class UpdateUserRequest {
        private String displayName;
        private String roles;
        private String departmentId;
        private Boolean active;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank private String newPassword;
    }

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<UserEntity>> getUsers(@RequestParam String tenantId) {
        List<UserEntity> users = userService.getUsers(tenantId);
        // Mask password hashes before returning
        users.forEach(u -> u.setPasswordHash("[protected]"));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<UserEntity> getUser(@RequestParam String tenantId, @PathVariable String userId) {
        return userService.getUserById(tenantId, userId)
                .map(u -> { u.setPasswordHash("[protected]"); return ResponseEntity.ok(u); })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<UserEntity> createUser(@Valid @RequestBody CreateUserRequest req) {
        log.info("Creating user: {} for tenant: {}", req.getUsername(), req.getTenantId());
        UserEntity created = userService.createUser(
                req.getTenantId(), req.getUsername(), req.getPassword(),
                req.getDisplayName(), req.getRoles(), req.getDepartmentId());
        created.setPasswordHash("[protected]");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<UserEntity> updateUser(
            @RequestParam String tenantId,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest req) {
        UserEntity updated = userService.updateUser(
                tenantId, userId, req.getDisplayName(),
                req.getRoles(), req.getDepartmentId(), req.getActive());
        updated.setPasswordHash("[protected]");
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam String tenantId,
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(tenantId, userId, req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
