package com.breadcost.notifications;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_templates",
    uniqueConstraints = @UniqueConstraint(name = "uq_template_tenant_type_channel",
        columnNames = {"tenantId", "type", "channel"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateEntity {

    @Id
    private String templateId;

    @Column(nullable = false)
    private String tenantId;

    /** Template type: ORDER_CONFIRMATION, PRODUCTION_STARTED, READY_FOR_DELIVERY,
     *  OUT_FOR_DELIVERY, DELIVERED, PAYMENT_REMINDER, STOCK_ALERT, PROMOTIONAL */
    @Column(nullable = false)
    private String type;

    /** Channel: PUSH, EMAIL, WHATSAPP, SMS */
    @Column(nullable = false)
    private String channel;

    private String subject;

    /** Supports {{orderNumber}}, {{customerName}}, {{status}} variables */
    @Column(length = 4000)
    private String bodyTemplate;

    @Builder.Default
    private boolean active = true;

    private long createdAtEpochMs;
    private long updatedAtEpochMs;

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAtEpochMs = now;
        this.updatedAtEpochMs = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAtEpochMs = System.currentTimeMillis();
    }
}
