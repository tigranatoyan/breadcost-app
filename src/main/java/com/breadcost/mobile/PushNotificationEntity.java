package com.breadcost.mobile;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "push_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PushNotificationEntity {

    @Id
    private String notificationId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private String notificationType; // ORDER_STATUS, LOYALTY, PROMOTION

    private String referenceId;

    @Builder.Default
    private String status = "PENDING";

    private Instant sentAt;
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
