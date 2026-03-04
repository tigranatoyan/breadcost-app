package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = {"tenantId", "username"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    private String userId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    /** Comma-separated roles: Admin,ProductionUser etc. */
    @Column(length = 500)
    private String roles;

    /** Optional department assignment */
    private String departmentId;

    @Builder.Default
    private boolean active = true;

    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public List<String> getRoleList() {
        if (roles == null || roles.isBlank()) return List.of();
        return List.of(roles.split(","));
    }
}
