package com.breadcost.mobile;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mobile_device_registrations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MobileDeviceRegistrationEntity {

    @Id
    private String registrationId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String deviceToken;

    @Column(nullable = false)
    private String platform; // IOS, ANDROID

    private String deviceName;

    @Builder.Default
    private boolean active = true;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
